package app.bpartners.geojobs.service.dashboard;

import static app.bpartners.geojobs.service.dashboard.ApiConfiguration.API_KEY_HEADER;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PUT;

import app.bpartners.geojobs.service.dashboard.component.AnalysisProject;
import app.bpartners.geojobs.service.dashboard.component.Register2d;
import app.bpartners.geojobs.service.dashboard.component.Register3d;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class AnalysisProjectApi {
  private final RestTemplate restTemplate = new RestTemplate();
  private final ApiConfiguration apiConfiguration;
  private final SecurityApi securityApi;

  public AnalysisProject register2d(String apiKey, String projectKey, Register2d register2d) {
    return exchange(apiKey, PUT, "/analysisProjects/" + projectKey + "/2d", register2d);
  }

  public AnalysisProject register3d(String apiKey, String projectKey, Register3d register3d) {
    return exchange(apiKey, PUT, "/analysisProjects/" + projectKey + "/3d", register3d);
  }

  public AnalysisProject get(String apiKey, String projectKey) {
    return exchange(apiKey, GET, "/analysisProjects/" + projectKey, null);
  }

  private AnalysisProject exchange(String apiKey, HttpMethod method, String path, Object body) {
    var dashboardUserId = securityApi.retrieveUserId(apiKey);
    var endpoint =
        String.format(
            "%s/users/%s%s", apiConfiguration.getDashboardApiUrl(), dashboardUserId, path);
    var headers = new HttpHeaders();
    headers.add(API_KEY_HEADER, apiKey);
    return restTemplate
        .exchange(endpoint, method, new HttpEntity<>(body, headers), AnalysisProject.class)
        .getBody();
  }
}
