package app.bpartners.geojobs.service.detection;

import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import app.bpartners.geojobs.file.BucketCustomizedComponent;
import app.bpartners.geojobs.file.ImageJpegCompressor;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
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
  public static final float IMAGE_QUALITY = 0.8f;
  private final ObjectMapper om;
  private final BucketCustomizedComponent bucketComponent;
  private final String tileDetectionRawBaseUrls;
  private final ImageJpegCompressor imageJpegCompressor;

  public HttpApiTileObjectDetector(
      ObjectMapper om,
      BucketCustomizedComponent bucketComponent,
      @Value("${tile.detection.api.urls}") String tileDetectionRawBaseUrls,
      ImageJpegCompressor imageJpegCompressor) {
    this.om = om;
    this.bucketComponent = bucketComponent;
    this.tileDetectionRawBaseUrls = tileDetectionRawBaseUrls;
    this.imageJpegCompressor = imageJpegCompressor;
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
            .filter(tileDetectorUrl -> tileDetectorUrl.getObjectType().equals(type))
            .findAny();
    if (optionalBaseUrl.isEmpty()) {
      throw new ApiException(SERVER_EXCEPTION, "Unknown DetectableType " + type);
    }
    return optionalBaseUrl.get().getUrl();
  }

  @SneakyThrows
  @Override
  public DetectionResponse apply(
      TileDetectionTask tileDetectionTask, List<DetectableType> detectableTypes) {
    Tile tile = tileDetectionTask.getTile();
    if (tile == null) {
      return null;
    }
    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(APPLICATION_JSON);

    File file = bucketComponent.download("cannes-qgis-tiles", tile.getBucketPath());
    File compressedFile = imageJpegCompressor.apply(file, IMAGE_QUALITY);
    String base64ImgData =
        Base64.getEncoder().encodeToString(FileUtils.readFileToByteArray(compressedFile));

    var payload =
        DetectionPayload.builder()
            .projectName(tileDetectionTask.getJobId())
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
      return responseEntity.getBody();
    }
    throw new ApiException(SERVER_EXCEPTION, "Server error");
  }
}
