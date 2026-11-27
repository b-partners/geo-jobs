package app.bpartners.geojobs.service.roofer3dbag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import app.bpartners.geojobs.service.roofer3dbag.conf.ApiClientConfig;
import app.bpartners.geojobs.service.roofer3dbag.conf.RooferApiProperties;
import app.bpartners.geojobs.service.roofer3dbag.exception.RooferApiException;
import app.bpartners.geojobs.service.roofer3dbag.model.CityJsonGenerationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class Roofer3DBagApiClientTest {
  private static final String BASE_URL = "http://roofer.test";
  private static final String GENERATE_URL = BASE_URL + "/cityjson/generate";
  private static final String SUCCESS_BODY =
      "{\"cityJsonUrl\":\"https://bucket/result.json\",\"expirationDateTime\":\"2026-01-01T00:00:00Z\"}";

  RooferApiProperties properties = new RooferApiProperties(BASE_URL, "roofer-api-key");
  RestTemplate restTemplate = new ApiClientConfig(properties).rooferRestTemplate();
  MockRestServiceServer server;
  ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
  Roofer3DBagApiClient subject =
      new Roofer3DBagApiClient(restTemplate, properties, objectMapper, 0.66f);

  CityJsonGenerationRequest request =
      CityJsonGenerationRequest.builder()
          .geoJsonBuildingPresignedUrl("https://bucket/building.geojson")
          .lidarPresignedUrls(List.of("https://bucket/lidar.copc.laz"))
          .build();

  @BeforeEach
  void setup() {
    server = MockRestServiceServer.bindTo(restTemplate).build();
  }

  @Test
  void generate_city_json_posts_request_with_default_complexity_and_knn() {
    server
        .expect(requestTo(GENERATE_URL + "?complexityFactor=0.66&knn=10"))
        .andExpect(method(POST))
        .andExpect(header("x-api-key", "roofer-api-key"))
        .andExpect(
            jsonPath("$.geoJsonBuildingPresignedUrl").value("https://bucket/building.geojson"))
        .andExpect(jsonPath("$.lidarPresignedUrls[0]").value("https://bucket/lidar.copc.laz"))
        .andRespond(withSuccess(SUCCESS_BODY, APPLICATION_JSON));

    var actual = subject.generateCityJson(request, null, null);

    assertEquals("https://bucket/result.json", actual.getCityJsonUrl());
    assertEquals(Instant.parse("2026-01-01T00:00:00Z"), actual.getExpirationDateTime());
    server.verify();
  }

  @Test
  void generate_city_json_uses_custom_complexity_and_knn_when_given() {
    server
        .expect(requestTo(GENERATE_URL + "?complexityFactor=0.3&knn=25"))
        .andRespond(withSuccess(SUCCESS_BODY, APPLICATION_JSON));

    var actual = subject.generateCityJson(request, 0.3f, 25);

    assertEquals("https://bucket/result.json", actual.getCityJsonUrl());
    server.verify();
  }

  @Test
  void generate_city_json_without_any_complexity_sends_no_query_parameters() {
    var withoutDefaultComplexity =
        new Roofer3DBagApiClient(restTemplate, properties, objectMapper, null);
    server.expect(requestTo(GENERATE_URL)).andRespond(withSuccess(SUCCESS_BODY, APPLICATION_JSON));

    var actual = withoutDefaultComplexity.generateCityJson(request, null, null);

    assertEquals("https://bucket/result.json", actual.getCityJsonUrl());
    server.verify();
  }

  @Test
  void api_error_with_json_body_exposes_status_and_api_error() {
    server
        .expect(requestTo(GENERATE_URL + "?complexityFactor=0.66&knn=10"))
        .andRespond(
            withStatus(BAD_REQUEST)
                .body("{\"error\":\"lidar file unreadable\"}")
                .contentType(APPLICATION_JSON));

    var error =
        assertThrows(RooferApiException.class, () -> subject.generateCityJson(request, null, null));

    assertEquals(BAD_REQUEST, error.getStatusCode());
    assertEquals("lidar file unreadable", error.getApiError());
    assertEquals(
        "API roofer error (HTTP 400 BAD_REQUEST) : lidar file unreadable", error.getMessage());
  }

  @Test
  void api_error_with_plain_body_keeps_the_body_as_message() {
    server
        .expect(requestTo(GENERATE_URL + "?complexityFactor=0.66&knn=10"))
        .andRespond(withStatus(INTERNAL_SERVER_ERROR).body("gateway exploded"));

    var error =
        assertThrows(RooferApiException.class, () -> subject.generateCityJson(request, null, null));

    assertEquals(INTERNAL_SERVER_ERROR, error.getStatusCode());
    assertNull(error.getApiError());
    assertTrue(error.getMessage().endsWith("gateway exploded"));
  }

  @Test
  void api_error_with_empty_body_has_no_api_error() {
    server
        .expect(requestTo(GENERATE_URL + "?complexityFactor=0.66&knn=10"))
        .andRespond(withStatus(INTERNAL_SERVER_ERROR));

    var error =
        assertThrows(RooferApiException.class, () -> subject.generateCityJson(request, null, null));

    assertNull(error.getApiError());
    assertEquals("API roofer error (HTTP 500 INTERNAL_SERVER_ERROR) : ", error.getMessage());
  }

  @Test
  void network_failure_is_reported_as_unable_to_call_the_api() {
    server
        .expect(requestTo(GENERATE_URL + "?complexityFactor=0.66&knn=10"))
        .andRespond(withException(new IOException("connection reset")));

    var error =
        assertThrows(RooferApiException.class, () -> subject.generateCityJson(request, null, null));

    assertNull(error.getStatusCode());
    assertTrue(error.getMessage().startsWith("Unable to call 3D Bag roofer API"));
    assertTrue(error.getMessage().contains("connection reset"));
  }
}
