package app.bpartners.geojobs.endpoint.rest.security;

import static app.bpartners.geojobs.endpoint.rest.model.BPToitureModel.ModelNameEnum.BP_TOITURE;
import static app.bpartners.geojobs.endpoint.rest.security.authenticator.ApiKeyAuthenticator.API_KEY_HEADER;
import static app.bpartners.geojobs.repository.model.SurfaceUnit.SQUARE_DEGREE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.ARBRE;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.rest.api.DetectionApi;
import app.bpartners.geojobs.endpoint.rest.api.MachineDetectionApi;
import app.bpartners.geojobs.endpoint.rest.client.ApiClient;
import app.bpartners.geojobs.endpoint.rest.client.ApiException;
import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.community.CommunityDetectableObjectType;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;

class CommunityAuthenticatedAccessIT extends FacadeIT {
  private static final String APIKEY = "APIKEY";

  MachineDetectionApi machineDetectionApi;
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
  void community_cannot_access_endpoint_if_not_detection() {
    var error = assertThrows(ApiException.class, () -> machineDetectionApi.getDetectionJobs(1, 10));
    assertTrue(error.getMessage().contains("Access Denied"));
  }

  @Test
  void community_cannot_do_detection_with_wrong_authorization() {
    var detectionId = randomUUID().toString();
    var detectableObjectModel = new DetectableObjectModel();
    detectableObjectModel.setActualInstance(new BPToitureModel().modelName(BP_TOITURE));
    var createDetection = new CreateDetection().detectableObjectModel(detectableObjectModel);
    var error =
        assertThrows(
            ApiException.class, () -> detectionApi.processDetection(detectionId, createDetection));
    assertTrue(error.getMessage().contains(ARBRE.name()));
  }

  void setupClientWithApiKey() {
    var authenticatedClient = new ApiClient();
    authenticatedClient.setRequestInterceptor(builder -> builder.header(API_KEY_HEADER, APIKEY));
    authenticatedClient.setScheme("http");
    authenticatedClient.setPort(port);
    authenticatedClient.setObjectMapper(om);

    detectionApi = new DetectionApi(authenticatedClient);
    machineDetectionApi = new MachineDetectionApi(authenticatedClient);
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
