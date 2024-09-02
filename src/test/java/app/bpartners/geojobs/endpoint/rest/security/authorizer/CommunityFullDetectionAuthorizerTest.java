package app.bpartners.geojobs.endpoint.rest.security.authorizer;

import static app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role.ROLE_ADMIN;
import static app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role.ROLE_COMMUNITY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.rest.model.CreateFullDetection;
import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectWithoutBucket;
import app.bpartners.geojobs.endpoint.rest.security.AuthProvider;
import app.bpartners.geojobs.endpoint.rest.security.model.Authority;
import app.bpartners.geojobs.endpoint.rest.security.model.Principal;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class CommunityFullDetectionAuthorizerTest {
  CommunityAuthorization communityAuthorization = mock();
  CreateFullDetection createFullDetection =
      new CreateFullDetection()
          .detectableObjectConfigurations(List.of(new DetectableObjectWithoutBucket()));
  CommunityZoneSurfaceAuthorizer communityZoneSurfaceAuthorizer = mock();
  CommunityZoneAuthorizer communityZoneAuthorizer = mock();
  CommunityDetectableObjectTypeAuthorizer communityDetectableObjectTypeAuthorizer = mock();
  CommunityAuthorizationRepository caRepository = mock();
  CommunityFullDetectionAuthorizer subject =
      new CommunityFullDetectionAuthorizer(
          communityDetectableObjectTypeAuthorizer,
          caRepository,
          communityZoneAuthorizer,
          communityZoneSurfaceAuthorizer);
  private final MockedStatic<AuthProvider> authProvider = mockStatic(AuthProvider.class);

  @BeforeEach
  void setup() {
    when(caRepository.findByApiKey(any())).thenReturn(Optional.of(communityAuthorization));
  }

  @AfterEach
  void cleanMock() {
    authProvider.close();
  }

  @Test
  void should_accept_directly_admin_api_key() {
    useRole(ROLE_ADMIN);
    when(communityAuthorization.getAuthorizedZones()).thenReturn(List.of());

    assertDoesNotThrow(
        () -> {
          subject.accept(createFullDetection);
        });
  }

  @Test
  void should_accept_community_if_authorization_is_correct() {
    useRole(ROLE_COMMUNITY);
    doNothing().when(communityZoneSurfaceAuthorizer).accept(any(), any());
    doNothing().when(communityZoneAuthorizer).accept(any(), any());
    doNothing().when(communityDetectableObjectTypeAuthorizer).accept(any(), any());

    assertDoesNotThrow(
        () -> {
          subject.accept(createFullDetection);
        });
  }

  private void useRole(Authority.Role role) {
    var userPrincipal = new Principal("dummyKey", Set.of(new Authority(role)));
    authProvider.when(AuthProvider::getPrincipal).thenReturn(userPrincipal);
  }
}
