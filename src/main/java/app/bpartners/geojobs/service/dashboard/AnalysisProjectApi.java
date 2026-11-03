package app.bpartners.geojobs.service.dashboard;

import static app.bpartners.geojobs.service.dashboard.ApiConfiguration.API_KEY_HEADER;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PUT;

import app.bpartners.geojobs.service.dashboard.component.AnalysisProject;
import app.bpartners.geojobs.service.dashboard.component.Register2d;
import app.bpartners.geojobs.service.dashboard.component.Register3d;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class AnalysisProjectApi {
  private static final String USERS_SEGMENT = "users";
  private static final String PROJECTS_SEGMENT = "analysisProjects";
  private static final String ANALYSIS_2D_SEGMENT = "2d";
  private static final String MODEL_3D_SEGMENT = "3d";
  private final RestTemplate restTemplate = new RestTemplate();
  private final ApiConfiguration apiConfiguration;
  private final SecurityApi securityApi;

  public AnalysisProject register2d(String apiKey, String projectKey, Register2d register2d) {
    return exchange(apiKey, PUT, projectKey, ANALYSIS_2D_SEGMENT, register2d);
  }

  public AnalysisProject register3d(String apiKey, String projectKey, Register3d register3d) {
    return exchange(apiKey, PUT, projectKey, MODEL_3D_SEGMENT, register3d);
  }

  public AnalysisProject get(String apiKey, String projectKey) {
    return exchange(apiKey, GET, projectKey, null, null);
  }

  static URI endpoint(
      String dashboardApiUrl, String dashboardUserId, String projectKey, String stepSegment) {
    if (projectKey == null
        || projectKey.isBlank()
        || ".".equals(projectKey)
        || "..".equals(projectKey)) {
      throw new IllegalArgumentException("Invalid analysis project key");
    }
    var builder =
        UriComponentsBuilder.fromUriString(dashboardApiUrl)
            .pathSegment(USERS_SEGMENT, dashboardUserId, PROJECTS_SEGMENT, projectKey);
    if (stepSegment != null) {
      builder.pathSegment(stepSegment);
    }
    return builder.build().encode().toUri();
  }

  private AnalysisProject exchange(
      String apiKey, HttpMethod method, String projectKey, String stepSegment, Object body) {
    var dashboardUserId = securityApi.retrieveUserId(apiKey);
    var endpoint =
        endpoint(apiConfiguration.getDashboardApiUrl(), dashboardUserId, projectKey, stepSegment);
    var headers = new HttpHeaders();
    headers.add(API_KEY_HEADER, apiKey);
    return restTemplate
        .exchange(endpoint, method, new HttpEntity<>(body, headers), AnalysisProject.class)
        .getBody();
  }
}
