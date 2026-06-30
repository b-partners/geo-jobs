package app.bpartners.geojobs.service.ciytjsonprocessor;

import static app.bpartners.geojobs.service.ciytjsonprocessor.model.ProcessingStatus.SUCCEEDED;
import static org.springframework.web.util.UriComponentsBuilder.fromUri;

import app.bpartners.geojobs.service.ciytjsonprocessor.conf.CityJsonProcessorApiProperties;
import app.bpartners.geojobs.service.ciytjsonprocessor.exception.CityJsonProcessorApiException;
import app.bpartners.geojobs.service.ciytjsonprocessor.model.CityJsonProcessorResponse;
import app.bpartners.geojobs.service.ciytjsonprocessor.model.CreateCityJsonFromFeatureFileUrl;
import app.bpartners.geojobs.service.ciytjsonprocessor.model.Problem;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class CityJsonProcessorApiClient {
  private final ObjectMapper objectMapper;
  private final RestTemplate restTemplate;
  private final CityJsonProcessorApiProperties properties;
  private static final String PREFIX_PATH = "/cityjsons";
  private static final String SUFFIX_PATH = "/feature-file";

  public CityJsonProcessorApiClient(
      @Qualifier("cityJsonProcessorRestTemplate") RestTemplate restTemplate,
      CityJsonProcessorApiProperties properties,
      ObjectMapper objectMapper) {
    this.restTemplate = restTemplate;
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  public CityJsonProcessorResponse generate(String id, CreateCityJsonFromFeatureFileUrl request) {
    var uri = buildGenerateUri(id);

    var headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

    var entity = new HttpEntity<>(request, headers);

    try {
      var response =
          restTemplate.exchange(uri, HttpMethod.PUT, entity, CityJsonProcessorResponse.class);
      var responseBody = response.getBody();
      if (responseBody != null && SUCCEEDED.equals(responseBody.getStatus())) {
        throw new RestClientException("CityJSONProcessorApi return FAILED status");
      }
      return responseBody;
    } catch (HttpStatusCodeException e) {
      throw mapHttpError(e);
    } catch (RestClientException e) {
      throw new CityJsonProcessorApiException(
          "Unable to call CityJsonProcessor API : " + e.getMessage(), e);
    }
  }

  @SneakyThrows
  private URI buildGenerateUri(String id) {
    var url = String.format("%s/%s/%s", PREFIX_PATH, id, SUFFIX_PATH);
    var builder = fromUri(new URI(properties.getBaseUrl())).path(url);
    return builder.build().toUri();
  }

  private CityJsonProcessorApiException mapHttpError(HttpStatusCodeException e) {
    String body = e.getResponseBodyAsString();
    String apiError = null;
    try {
      if (!body.isBlank()) {
        var parsed = objectMapper.readValue(body, Problem.class);
        apiError = parsed.getDetail();
      }
    } catch (Exception parseEx) {
      log.debug("Unable to parse response : {}", parseEx.getMessage());
    }

    var message =
        String.format(
            "API roofer error (HTTP %s) : %s",
            e.getStatusCode(), apiError != null ? apiError : body);
    return new CityJsonProcessorApiException(e.getStatusCode(), apiError, message);
  }
}
