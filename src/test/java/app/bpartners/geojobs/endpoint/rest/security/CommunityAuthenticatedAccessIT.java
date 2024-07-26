package app.bpartners.geojobs.endpoint.rest.security;

import static app.bpartners.geojobs.endpoint.rest.security.authenticator.ApiKeyAuthenticator.API_KEY_HEADER;
import static org.junit.jupiter.api.Assertions.*;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.rest.api.DetectionApi;
import app.bpartners.geojobs.endpoint.rest.client.ApiClient;
import app.bpartners.geojobs.endpoint.rest.client.ApiException;
import app.bpartners.geojobs.endpoint.rest.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;

public class CommunityAuthenticatedAccessIT extends FacadeIT {
  DetectionApi detectionApi;

  @Autowired ObjectMapper om;
  @LocalServerPort private int port;

  @BeforeEach
  void setup() {
    setupClientWithApiKey();
  }

  void setupClientWithApiKey() {
    var authenticatedClient = new ApiClient();
    authenticatedClient.setRequestInterceptor(
        builder -> builder.header(API_KEY_HEADER, "community1_key"));
    authenticatedClient.setScheme("http");
    authenticatedClient.setPort(port);
    authenticatedClient.setObjectMapper(om);

    detectionApi = new DetectionApi(authenticatedClient);
  }

  @Test
  void community_cannot_access_endpoint_if_not_full_detection() {
    var error =
        assertThrows(
            ApiException.class,
            () -> {
              detectionApi.getDetectionJobs(1, 10);
            });
    assertEquals("ricka", error.getMessage());
  }

  @Test
  void community_cannot_do_full_detection_with_not_authorized_object_type() {
    var error =
        assertThrows(
            ApiException.class,
            () -> {
              detectionApi.processFullDetection(asCreateFullDetections(List.of()));
            });
    assertEquals("ricka", error.getMessage());
  }

  @Test
  void community_can_do_full_detection_with_correct_authorization() throws ApiException {
    assertDoesNotThrow(
        () -> {
          detectionApi.processFullDetection(asCreateFullDetections(List.of()));
        });
  }

  private List<CreateFullDetection> asCreateFullDetections(
      List<DetectableObjectType> authorizedTypes) {
    return null;
  }

  private static Feature feature2000Surface() {
    Feature feature = new Feature();
    var coordinates =
        List.of(
            List.of(
                List.of(BigDecimal.valueOf(0), BigDecimal.valueOf(0)),
                List.of(BigDecimal.valueOf(0), BigDecimal.valueOf(44.72)),
                List.of(BigDecimal.valueOf(44.72), BigDecimal.valueOf(44.72)),
                List.of(BigDecimal.valueOf(44.72), BigDecimal.valueOf(0)),
                List.of(BigDecimal.valueOf(0), BigDecimal.valueOf(0))));
    MultiPolygon multiPolygon = new MultiPolygon().coordinates(List.of(coordinates));
    feature.setGeometry(multiPolygon);
    return feature;
  }
}
