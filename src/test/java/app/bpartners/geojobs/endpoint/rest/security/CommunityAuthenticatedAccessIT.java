package app.bpartners.geojobs.endpoint.rest.security;

import static app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType.PISCINE;
import static app.bpartners.geojobs.endpoint.rest.security.authenticator.ApiKeyAuthenticator.API_KEY_HEADER;
import static app.bpartners.geojobs.repository.model.SurfaceUnit.SQUARE_DEGREE;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.rest.api.DetectionApi;
import app.bpartners.geojobs.endpoint.rest.client.ApiClient;
import app.bpartners.geojobs.endpoint.rest.client.ApiException;
import app.bpartners.geojobs.endpoint.rest.model.CreateDetection;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.community.CommunityDetectableObjectType;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;

@Disabled("TODO: fail")
class CommunityAuthenticatedAccessIT extends FacadeIT {
  private static final String APIKEY = "APIKEY";

  DetectionApi detectionApi;

  @Autowired ObjectMapper om;
  @Autowired CommunityAuthorizationRepository caRepository;

  @LocalServerPort private int port;

  @BeforeEach
  void setup() {
    caRepository.save(communityAuthorization());
    setupClientWithApiKey();
  }

  @AfterEach
  void cleanup() {
    caRepository.deleteAll();
  }

  @Test
  void community_cannot_access_endpoint_if_not_full_detection() {
    var error = assertThrows(ApiException.class, () -> detectionApi.getDetectionJobs(1, 10));
    assertTrue(error.getMessage().contains("Access Denied"));
  }

  @Test
  void community_cannot_do_full_detection_with_wrong_authorization() {
    var error =
        assertThrows(
            ApiException.class,
            () ->
                detectionApi.processDetection(
                    randomUUID().toString(),
                    new CreateDetection().geoJsonZone(List.of(new Feature()))
                    // TODO .objectType(PISCINE)
                    ));
    assertTrue(error.getMessage().contains(PISCINE.name()));
  }

  @Test
  void community_cannot_do_full_detection_if_no_e2e_id_provided() {
    var error =
        assertThrows(
            ApiException.class,
            () -> detectionApi.processDetection(randomUUID().toString(), new CreateDetection()));
    assertEquals(BAD_REQUEST.value(), error.getCode());
    assertTrue(error.getMessage().contains("You must provide an end-to-end id for your detection"));
  }

  @Test
  void community_cannot_do_full_detection_if_no_features_provided() {
    var error =
        assertThrows(
            ApiException.class,
            () ->
                detectionApi.processDetection(
                    randomUUID().toString(), new CreateDetection()
                    // TODO: set .objectType(PISCINE)
                    ));
    assertEquals(BAD_REQUEST.value(), error.getCode());
    assertTrue(error.getMessage().contains("You must provide features for your detection"));
  }

  void setupClientWithApiKey() {
    var authenticatedClient = new ApiClient();
    authenticatedClient.setRequestInterceptor(builder -> builder.header(API_KEY_HEADER, APIKEY));
    authenticatedClient.setScheme("http");
    authenticatedClient.setPort(port);
    authenticatedClient.setObjectMapper(om);

    detectionApi = new DetectionApi(authenticatedClient);
  }

  private CommunityAuthorization communityAuthorization() {
    var communityDetectableType =
        CommunityDetectableObjectType.builder()
            .id("dummyId")
            .type(DetectableType.PASSAGE_PIETON)
            .communityAuthorizationId("dummyId")
            .build();

    return CommunityAuthorization.builder()
        .id("dummyId")
        .apiKey(APIKEY)
        .name("communityName")
        .maxSurface(5_000)
        .maxSurfaceUnit(SQUARE_DEGREE)
        .authorizedZones(List.of())
        .usedSurfaces(List.of())
        .detectableObjectTypes(List.of(communityDetectableType))
        .build();
  }
}
