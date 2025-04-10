package app.bpartners.geojobs.service.dashboard;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

import app.bpartners.geojobs.service.dashboard.component.CreateDetectionTracking;
import app.bpartners.geojobs.service.dashboard.component.DetectionTracking;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class DetectionTrackingApi {
  private static final String API_KEY_HEADER = "x-api-key";
  private final RestTemplate restTemplate;
  private final String dashboardApiUrl;
  private final ObjectMapper objectMapper;

  public DetectionTrackingApi(
      @Value("${bpartners.api.url}") String dashboardApiUrl, ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    this.restTemplate = new RestTemplate();
    this.dashboardApiUrl = dashboardApiUrl;
  }

  public List<DetectionTracking> registerDetection(
      String apiKey, List<CreateDetectionTracking> createDetectionTracking) {
    var dashboardUserId = retrieveUserId(apiKey);
    var endpoint = String.format("%s/users/%s/detectionTracking", dashboardApiUrl, dashboardUserId);
    var headers = new HttpHeaders();
    headers.add(API_KEY_HEADER, apiKey);
    var requestEntity = new HttpEntity<>(createDetectionTracking, headers);

    return restTemplate
        .exchange(
            endpoint,
            POST,
            requestEntity,
            new ParameterizedTypeReference<List<DetectionTracking>>() {})
        .getBody();
  }

  @SneakyThrows
  private String retrieveUserId(String apiKey) {
    var endpoint = String.format("%s/whoami", dashboardApiUrl);
    var headers = new HttpHeaders();
    headers.add(API_KEY_HEADER, apiKey);
    var requestEntity = new HttpEntity<>(headers);

    var responseBody = restTemplate.exchange(endpoint, GET, requestEntity, String.class).getBody();

    var root = objectMapper.readTree(responseBody);
    return root.path("user").path("id").asText();
  }
}
