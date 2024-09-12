package app.bpartners.geojobs.endpoint.rest.security.authorizer;

import static app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role.ROLE_ADMIN;
import static app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role.ROLE_COMMUNITY;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.rest.model.CreateFullDetection;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.security.model.Authority;
import app.bpartners.geojobs.endpoint.rest.security.model.Principal;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FullDetectionAuthorizerTest {
  CommunityAuthorization communityAuthorization = mock();
  CreateFullDetection createFullDetection = mock();
  CommunityZoneSurfaceAuthorizer communityZoneSurfaceAuthorizer = mock();
  CommunityZoneAuthorizer communityZoneAuthorizer = mock();
  CommunityDetectableObjectTypeAuthorizer communityDetectableObjectTypeAuthorizer = mock();
  CommunityAuthorizationRepository caRepository = mock();
  FullDetectionAuthorizer subject =
      new FullDetectionAuthorizer(
          communityDetectableObjectTypeAuthorizer,
          caRepository,
          communityZoneAuthorizer,
          communityZoneSurfaceAuthorizer);

  @BeforeEach
  void setup() {
    when(caRepository.findByApiKey(any())).thenReturn(Optional.of(communityAuthorization));
    when(createFullDetection.getFeatures()).thenReturn(List.of(mock(Feature.class)));
  }

  @Test
  void should_accept_directly_admin_api_key() {
    when(communityAuthorization.getAuthorizedZones()).thenReturn(List.of());

    assertDoesNotThrow(() -> subject.accept(createFullDetection, useRole(ROLE_ADMIN)));
  }

  @Test
  void should_accept_community_if_authorization_is_correct() {
    doNothing().when(communityZoneSurfaceAuthorizer).accept(any(), any());
    doNothing().when(communityZoneAuthorizer).accept(any(), any());
    doNothing().when(communityDetectableObjectTypeAuthorizer).accept(any(), any());

    assertDoesNotThrow(() -> subject.accept(createFullDetection, useRole(ROLE_COMMUNITY)));
  }

  private Principal useRole(Authority.Role role) {
    return new Principal("dummyKey", Set.of(new Authority(role)));
  }
}
