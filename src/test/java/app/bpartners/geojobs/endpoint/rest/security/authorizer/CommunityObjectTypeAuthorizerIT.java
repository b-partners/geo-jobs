package app.bpartners.geojobs.endpoint.rest.security.authorizer;

import static app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role.*;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType;
import app.bpartners.geojobs.endpoint.rest.security.AuthProvider;
import app.bpartners.geojobs.endpoint.rest.security.model.Authority;
import app.bpartners.geojobs.endpoint.rest.security.model.Principal;
import app.bpartners.geojobs.model.exception.ForbiddenException;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.CommunityDetectableObjectType;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class CommunityObjectTypeAuthorizerIT extends FacadeIT {
  private final MockedStatic<AuthProvider> authProvider = mockStatic(AuthProvider.class);
  @MockBean CommunityAuthorizationRepository caRepository;
  @Autowired CommunityObjectTypeAuthorizer communityObjectTypeAuthorizer;

  @Test
  void should_accept_directly_admin_key() {
    useRole(ROLE_ADMIN);
    assertDoesNotThrow(
        () ->
            communityObjectTypeAuthorizer.accept(
                List.of(DetectableObjectType.PATHWAY, DetectableObjectType.ROOF)));
  }

  @Test
  void should_throws_if_not_authorized_object_type() {
    useRole(ROLE_COMMUNITY);
    when(caRepository.findByApiKey(any()))
        .thenReturn(asOptionalCommunityAuthorization(List.of(POOL)));

    var error =
        assertThrows(
            ForbiddenException.class,
            () ->
                communityObjectTypeAuthorizer.accept(
                    List.of(
                        DetectableObjectType.PATHWAY,
                        DetectableObjectType.ROOF,
                        DetectableObjectType.POOL)));
    assertTrue(error.getMessage().contains("PATHWAY"));
    assertTrue(error.getMessage().contains("ROOF"));
    assertFalse(error.getMessage().contains("POOL")); // pool is allowed
  }

  @Test
  void should_accept_community_with_correct_object_type() {
    useRole(ROLE_COMMUNITY);
    when(caRepository.findByApiKey(any()))
        .thenReturn(asOptionalCommunityAuthorization(List.of(PATHWAY, ROOF, POOL)));

    assertDoesNotThrow(
        () -> {
          communityObjectTypeAuthorizer.accept(
              List.of(DetectableObjectType.PATHWAY, DetectableObjectType.ROOF));
          communityObjectTypeAuthorizer.accept(
              List.of(
                  DetectableObjectType.PATHWAY,
                  DetectableObjectType.ROOF,
                  DetectableObjectType.POOL));
        });
  }

  private Optional<CommunityAuthorization> asOptionalCommunityAuthorization(
      List<DetectableType> detectableObjectTypes) {
    var communityDetectableObjectTypes =
        detectableObjectTypes.stream()
            .map(
                detectableType ->
                    CommunityDetectableObjectType.builder().type(detectableType).build())
            .toList();

    return Optional.of(
        CommunityAuthorization.builder()
            .detectableObjectTypes(communityDetectableObjectTypes)
            .build());
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
