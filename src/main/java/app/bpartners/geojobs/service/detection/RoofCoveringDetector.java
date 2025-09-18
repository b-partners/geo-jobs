package app.bpartners.geojobs.service.detection;

import static org.apache.commons.io.FileUtils.readFileToByteArray;

import app.bpartners.geojobs.file.bucket.CustomBucketComponent;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@Slf4j
public class RoofCoveringDetector {
  private final ObjectMapper om;
  private final RestTemplate restTemplate;
  private final String roofCoveringDetectionApiUrl;
  private final String roofCoveringDetectionApiToken;
  private final CustomBucketComponent bucketComponent;

  public RoofCoveringDetector(
      ObjectMapper om,
      RestTemplate restTemplate,
      @Value("${roof.covering.detection.api.url}") String roofCoveringDetectionApiUrl,
      @Value("${roof.covering.detection.api.token}") String roofCoveringDetectionApiToken,
      CustomBucketComponent bucketComponent) {
    this.om = om;
    this.restTemplate = restTemplate;
    this.roofCoveringDetectionApiUrl = roofCoveringDetectionApiUrl;
    this.roofCoveringDetectionApiToken = roofCoveringDetectionApiToken;
    this.bucketComponent = bucketComponent;
  }

  @SneakyThrows
  public RoofCoveringDetectionResponse apply(Tile tile, File mask) {
    File file =
        bucketComponent.download(
            bucketComponent.getBucketConf().getBucketName(), tile.getBucketPath());
    String imageBase64 = Base64.getEncoder().encodeToString(readFileToByteArray(file));
    String maskBase64 =
        mask == null ? null : Base64.getEncoder().encodeToString(readFileToByteArray(mask));

    return apply(imageBase64, maskBase64);
  }

  @SneakyThrows
  public RoofCoveringDetectionResponse apply(String imageBase64, String maskBase64) {

    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Bearer " + roofCoveringDetectionApiToken);
    headers.add("Content-Type", "application/json");

    RoofCoveringDetectionPayload payload =
        new RoofCoveringDetectionPayload(imageBase64, maskBase64);

    String requestBody = om.writeValueAsString(payload);

    HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

    UriComponentsBuilder uriBuilder;
    ResponseEntity<RoofCoveringDetectionResponse> responseEntity;

    try {
      uriBuilder =
          UriComponentsBuilder.fromUri(new URI(roofCoveringDetectionApiUrl + "Prod/coatings"));
      responseEntity =
          restTemplate.postForEntity(
              uriBuilder.toUriString(), request, RoofCoveringDetectionResponse.class);

      if (responseEntity.getStatusCode().value() != 200) {
        log.info(
            "Got HTTP code {} while calling API for roof covering detection {}",
            responseEntity.getStatusCode(),
            roofCoveringDetectionApiUrl);
      }

      return responseEntity.getBody();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    } catch (HttpStatusCodeException e) {
      log.error(
          "Error while calling API for roof covering detection {} with exception {}",
          roofCoveringDetectionApiUrl,
          e.getMessage());
    }

    return null;
  }
}
