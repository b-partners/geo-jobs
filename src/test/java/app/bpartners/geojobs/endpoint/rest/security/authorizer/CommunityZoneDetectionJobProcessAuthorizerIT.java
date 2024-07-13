package app.bpartners.geojobs.endpoint.rest.security.authorizer;

import static app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role.ROLE_ADMIN;
import static app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role.ROLE_COMMUNITY;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectConfiguration;
import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType;
import app.bpartners.geojobs.endpoint.rest.security.AuthProvider;
import app.bpartners.geojobs.endpoint.rest.security.model.Authority;
import app.bpartners.geojobs.endpoint.rest.security.model.Principal;
import app.bpartners.geojobs.model.CommunityAuthorizationDetails;
import app.bpartners.geojobs.model.exception.ForbiddenException;
import app.bpartners.geojobs.repository.impl.CommunityAuthorizationDetailsRepositoryImpl;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class CommunityZoneDetectionJobProcessAuthorizerIT extends FacadeIT {
  private final MockedStatic<AuthProvider> authProvider = mockStatic(AuthProvider.class);
  @MockBean CommunityAuthorizationDetailsRepositoryImpl cadRepository;
  @Autowired CommunityZoneDetectionJobProcessAuthorizer communityZoneDetectionJobProcessAuthorizer;

  @Test
  void should_accept_directly_admin_key() {
    useRole(ROLE_ADMIN);
    assertDoesNotThrow(
        () ->
            communityZoneDetectionJobProcessAuthorizer.accept(
                "dummyJobId",
                asDetectableObjectConfiguration(
                    List.of(DetectableObjectType.PATHWAY, DetectableObjectType.ROOF))));
  }

  @Test
  void community_cannot_detect_not_authorized_object_type() {
    useRole(ROLE_COMMUNITY);
    when(cadRepository.findByApiKey(any()))
        .thenReturn(asCommunityAuthorizationDetails(List.of(POOL)));

    var error =
        assertThrows(
            ForbiddenException.class,
            () -> {
              communityZoneDetectionJobProcessAuthorizer.accept(
                  "dummyJobId",
                  asDetectableObjectConfiguration(
                      List.of(DetectableObjectType.PATHWAY, DetectableObjectType.ROOF)));
            });
    assertTrue(error.getMessage().contains("PATHWAY"));
    assertTrue(error.getMessage().contains("ROOF"));
  }

  @Test
  void should_accept_community_with_correct_permissions() {
    useRole(ROLE_COMMUNITY);
    when(cadRepository.findByApiKey(any()))
        .thenReturn(asCommunityAuthorizationDetails(List.of(PATHWAY, ROOF, POOL)));

    assertDoesNotThrow(
        () -> {
          communityZoneDetectionJobProcessAuthorizer.accept(
              "dummyJobId",
              asDetectableObjectConfiguration(
                  List.of(DetectableObjectType.PATHWAY, DetectableObjectType.ROOF)));
          communityZoneDetectionJobProcessAuthorizer.accept(
              "dummyJobId",
              asDetectableObjectConfiguration(
                  List.of(
                      DetectableObjectType.PATHWAY,
                      DetectableObjectType.ROOF,
                      DetectableObjectType.POOL)));
        });
  }

  private CommunityAuthorizationDetails asCommunityAuthorizationDetails(
      List<DetectableType> detectableObjectTypes) {
    return new CommunityAuthorizationDetails(
        "dummy_id",
        "dummy_name",
        "dummy_name",
        5_000,
        List.of("dummy_zone_name"),
        detectableObjectTypes);
  }

  public List<DetectableObjectConfiguration> asDetectableObjectConfiguration(
      List<DetectableObjectType> detectableObjectTypes) {
    return detectableObjectTypes.stream()
        .map(
            objetType ->
                new DetectableObjectConfiguration().type(objetType).confidence(BigDecimal.TEN))
        .toList();
  }

  void useRole(Authority.Role role) {
    var userPrincipal = new Principal("dummy-api-key", Set.of(new Authority(role)));
    authProvider.when(AuthProvider::getPrincipal).thenReturn(userPrincipal);
  }

  @AfterEach
  void cleanMock() {
    authProvider.close();
  }
}
