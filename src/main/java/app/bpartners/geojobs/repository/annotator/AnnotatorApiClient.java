package app.bpartners.geojobs.repository.annotator;

import app.bpartners.geojobs.repository.annotator.exception.AnnotatorClientException;
import app.bpartners.geojobs.repository.annotator.gen.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AnnotatorApiClient {
  private final ObjectMapper om = new ObjectMapper().findAndRegisterModules();
  @Getter private final String annotatorApiUrl;
  private final HttpClient httpClient;

  public AnnotatorApiClient(
      @Value("${annotator.api.url}") String annotatorApiUrl, HttpClient httpClient) {
    this.annotatorApiUrl = annotatorApiUrl;
    this.httpClient = httpClient;
  }

  @Autowired
  public AnnotatorApiClient(@Value("${annotator.api.url}") String annotatorApiUrl) {
    this.annotatorApiUrl = annotatorApiUrl;
    httpClient = HttpClient.newBuilder().build();
  }

  public Job crupdateAnnotatedJob(CrupdateAnnotatedJob job) {
    try {
      String body = om.writeValueAsString(job);
      String pathUrl = annotatorApiUrl + "/annotated-jobs/" + job.getId();
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(new URI(pathUrl))
              .PUT(HttpRequest.BodyPublishers.ofString(body))
              .build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      String responseBody = response.body();
      if (response.statusCode() != 200 && response.statusCode() != 200) {
        throw new AnnotatorClientException(responseBody);
      }
      return om.readValue(responseBody, new TypeReference<>() {});
    } catch (IOException | InterruptedException | URISyntaxException e) {
      throw new AnnotatorClientException(e);
    }
  }

  public Job getJobById(String jobId) {
    try {
      String pathUrl = annotatorApiUrl + "/jobs/" + jobId;
      HttpRequest request = HttpRequest.newBuilder().uri(new URI(pathUrl)).GET().build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      String responseBody = response.body();
      if (response.statusCode() != 200 && response.statusCode() != 200) {
        throw new AnnotatorClientException(responseBody);
      }
      return om.readValue(responseBody, new TypeReference<>() {});
    } catch (IOException | InterruptedException | URISyntaxException e) {
      throw new AnnotatorClientException(e);
    }
  }

  public List<Task> getTasksByJobId(
      String jobId, String userId, TaskStatus status, Integer page, Integer pageSize) {
    try {
      String queryParamsValue = computeGetTaskByJobIdQueryParams(userId, status, page, pageSize);
      String pathUrl = annotatorApiUrl + "/jobs/" + jobId + "/tasks" + queryParamsValue;
      HttpRequest request = HttpRequest.newBuilder().uri(new URI(pathUrl)).GET().build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      String responseBody = response.body();
      if (response.statusCode() != 200 && response.statusCode() != 200) {
        throw new AnnotatorClientException(responseBody);
      }
      return om.readValue(responseBody, new TypeReference<>() {});
    } catch (IOException | InterruptedException | URISyntaxException e) {
      throw new AnnotatorClientException(e);
    }
  }

  public List<AnnotationBatch> getAnnotationBatches(
      String jobId, String taskId, Integer page, Integer pageSize) {
    try {
      String queryParamsValue = computeGetAnnotationBatchesQueryParams(page, pageSize);
      String pathUrl = annotatorApiUrl + "/jobs/" + jobId + "/tasks/" + taskId + queryParamsValue;
      HttpRequest request = HttpRequest.newBuilder().uri(new URI(pathUrl)).GET().build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      String responseBody = response.body();
      if (response.statusCode() != 200 && response.statusCode() != 200) {
        throw new AnnotatorClientException(responseBody);
      }
      return om.readValue(responseBody, new TypeReference<>() {});
    } catch (IOException | InterruptedException | URISyntaxException e) {
      throw new AnnotatorClientException(e);
    }
  }

  private String computeGetTaskByJobIdQueryParams(
      String userId, TaskStatus status, Integer page, Integer pageSize) {
    Map<String, String> queryParamsMap = new HashMap<>();
    if (userId != null) {
      queryParamsMap.put("userId", userId);
    }
    if (status != null) {
      queryParamsMap.put("status", status.toString());
    }
    if (page != null) {
      queryParamsMap.put("page", page.toString());
    }
    if (pageSize != null) {
      queryParamsMap.put("pageSize", pageSize.toString());
    }
    return queryParameters(queryParamsMap);
  }

  private String computeGetAnnotationBatchesQueryParams(Integer page, Integer pageSize) {
    Map<String, String> queryParamsMap = new HashMap<>();
    if (page != null) {
      queryParamsMap.put("page", page.toString());
    }
    if (pageSize != null) {
      queryParamsMap.put("pageSize", pageSize.toString());
    }
    return queryParameters(queryParamsMap);
  }

  private String queryParameters(Map<String, String> queryParams) {
    String queryStringValue =
        queryParams.entrySet().stream()
            .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
            .collect(Collectors.joining("&"));
    return queryParams.isEmpty() ? "" : "?" + queryStringValue;
  }
}
