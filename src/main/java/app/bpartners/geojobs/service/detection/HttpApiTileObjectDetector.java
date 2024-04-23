package app.bpartners.geojobs.service.detection;

import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import app.bpartners.geojobs.file.BucketComponent;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.repository.model.TileTask;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.Base64;
import java.util.List;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@ConditionalOnProperty(value = "objects.detector.mock.activated", havingValue = "false")
@Slf4j
public class HttpApiTileObjectDetector implements TileObjectDetector {
  private final ObjectMapper om;
  private final BucketComponent bucketComponent;
  private final String tileDetectionRawBaseUrls;

  public HttpApiTileObjectDetector(
      ObjectMapper om,
      BucketComponent bucketComponent,
      @Value("${tile.detection.api.urls}") String tileDetectionRawBaseUrls) {
    this.om = om;
    this.bucketComponent = bucketComponent;
    this.tileDetectionRawBaseUrls = tileDetectionRawBaseUrls;
  }

  private List<TileDetectorUrl> getDetectorUrls() {
    try {
      return om.readValue(tileDetectionRawBaseUrls, new TypeReference<>() {});
    } catch (JsonProcessingException e) {
      throw new ApiException(SERVER_EXCEPTION, e.getMessage());
    }
  }

  private String retrieveBaseUrl(List<DetectableType> types) {
    if (types.size() != 1) {
      throw new NotImplementedException(
          "Only one object detection per task is implemented for now but wanted detectable types"
              + " are "
              + types.size());
    }
    var type = types.getFirst();
    List<TileDetectorUrl> tileDetectionBaseUrls = getDetectorUrls();
    var optionalBaseUrl =
        tileDetectionBaseUrls.stream()
            .filter(
                tileDetectorUrl -> {
                  boolean isTileDetector = tileDetectorUrl.getObjectType().equals(type);
                  if (isTileDetector) {
                    log.info("[DEBUG] Objects detector chosen {}", tileDetectorUrl);
                  }
                  return isTileDetector;
                })
            .findAny();
    if (optionalBaseUrl.isEmpty()) {
      throw new ApiException(SERVER_EXCEPTION, "Unknown DetectableType " + type);
    }
    return optionalBaseUrl.get().getUrl();
  }

  @SneakyThrows
  @Override
  public DetectionResponse apply(TileTask tileTask, List<DetectableType> detectableTypes) {
    Tile tile = tileTask.getTile();
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
            .projectName(tileTask.getJobId())
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
      log.info("[DEBUG] Response data {}", responseEntity.getBody());
      return responseEntity.getBody();
    }
    log.info(
        "[DEBUG] Error when retrieving objects detector response, code={}, body={}",
        responseEntity.getStatusCode().value(),
        responseEntity.getBody());
    throw new ApiException(SERVER_EXCEPTION, "Server error");
  }
}
