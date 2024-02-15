package app.bpartners.geojobs.service.detection;

import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import app.bpartners.geojobs.file.self.BucketComponent;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.repository.model.detection.DetectionTask;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.Base64;
import java.util.function.Function;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@Slf4j
public class ObjectsDetector implements Function<DetectionTask, DetectionResponse> {
  private final String tileDetectionBaseUrl;
  private final ObjectMapper om;
  private final BucketComponent bucketComponent;

  public ObjectsDetector(
      @Value("${tile.detection.api.url}") String apiUrl,
      ObjectMapper objectMapper,
      BucketComponent bucket) {
    tileDetectionBaseUrl = apiUrl;
    om = objectMapper;
    bucketComponent = bucket;
  }

  @SneakyThrows
  @Override
  public DetectionResponse apply(DetectionTask task) {
    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(APPLICATION_JSON);

    Tile tile = task.getTile();
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
        UriComponentsBuilder.fromHttpUrl(tileDetectionBaseUrl + "/detection");
    ResponseEntity<DetectionResponse> responseEntity =
        restTemplate.postForEntity(builder.toUriString(), request, DetectionResponse.class);

    if (responseEntity.getStatusCode().value() == 200) {
      log.info("Response data {}", responseEntity.getBody());
      return responseEntity.getBody();
    }
    throw new ApiException(SERVER_EXCEPTION, "Server error");
  }
}
