package app.bpartners.geojobs.service.detection;

import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.*;
import static app.bpartners.geojobs.service.detection.HttpApiObjectsDetector.TileDetectorUrl.getDetectorUrls;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import app.bpartners.geojobs.file.BucketComponent;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import app.bpartners.geojobs.repository.model.detection.DetectionTask;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.Serializable;
import java.util.Base64;
import java.util.List;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@ConditionalOnProperty(value = "objects.detector.mock.activated", havingValue = "false")
@AllArgsConstructor
@Slf4j
public class HttpApiObjectsDetector implements ObjectsDetector {
  private final ObjectMapper om;
  private final BucketComponent bucketComponent;
  private final List<TileDetectorUrl> tileDetectionBaseUrls = getDetectorUrls();

  private String retrieveBaseUrl(List<DetectableType> types) {
    if (types.size() != 1) {
      throw new NotImplementedException(
          "Only one object detection per task is implemented for now but wanted detectable types"
              + " are "
              + types.size());
    }
    var type = types.getFirst();
    var optionalBaseUrl =
        tileDetectionBaseUrls.stream()
            .filter(tileDetectorUrl -> tileDetectorUrl.getObjectType().equals(type))
            .findAny();
    if (optionalBaseUrl.isEmpty()) {
      throw new ApiException(SERVER_EXCEPTION, "Unknown DetectableType " + type);
    }
    return optionalBaseUrl.get().getUrl();
  }

  @SneakyThrows
  @Override
  public DetectionResponse apply(DetectionTask task, List<DetectableType> detectableTypes) {
    Tile tile = task.getTile();
    if (tile == null) {
      return null;
    }
    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(APPLICATION_JSON);

    File file = bucketComponent.download(tile.getBucketPath());
    String base64ImgData = Base64.getEncoder().encodeToString(FileUtils.readFileToByteArray(file));

    var payload =
        DetectionPayload.builder()
            .projectName(task.getJobId())
            .fileName(file.getName())
            .base64ImgData(base64ImgData)
            .build();
    String requestBody = om.writeValueAsString(payload);

    HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

    UriComponentsBuilder builder =
        UriComponentsBuilder.fromHttpUrl(retrieveBaseUrl(detectableTypes) + "/detection");
    ResponseEntity<DetectionResponse> responseEntity =
        restTemplate.postForEntity(builder.toUriString(), request, DetectionResponse.class);

    if (responseEntity.getStatusCode().value() == 200) {
      log.error("[DEBUG] Response data {}", responseEntity.getBody());
      return responseEntity.getBody();
    }
    log.error(
        "[DEBUG] Error when retrieving objects detector response, code={}, body={}",
        responseEntity.getStatusCode().value(),
        responseEntity.getBody());
    throw new ApiException(SERVER_EXCEPTION, "Server error");
  }

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  static class TileDetectorUrl implements Serializable {
    @JsonProperty("objectType")
    private DetectableType objectType;

    @JsonProperty("url")
    private String url;

    // TODO: set it as env variables
    static List<TileDetectorUrl> getDetectorUrls() {
      return List.of(
          TileDetectorUrl.builder()
              .objectType(ROOF)
              .url("https://roof-api.azurewebsites.net/api")
              .build(),
          TileDetectorUrl.builder()
              .objectType(DetectableType.PATHWAY)
              .url("https://pathway-api.azurewebsites.net/api")
              .build(),
          TileDetectorUrl.builder()
              .objectType(SOLAR_PANEL)
              .url("https://solarpanel-api.azurewebsites.net/api")
              .build(),
          TileDetectorUrl.builder()
              .objectType(POOL)
              .url("https://pool-api.azurewebsites.net/api")
              .build(),
          TileDetectorUrl.builder()
              .objectType(TREE)
              .url("https://trees-api.azurewebsites.net/api")
              .build()
          /*
          TODO: add missing detectable types
          TileDetectorUrl.builder().objectType(SIDEWALK).url("https://sidewalk-api.azurewebsites.net/api").build(),
          TileDetectorUrl.builder().objectType(LINE).url("https://line-api.azurewebsites.net/api").build(),
          TileDetectorUrl.builder().objectType(GREEN_SPACE).url("https://greenspace-api.azurewebsites.net/api").build()*/
          );
    }
  }
}
