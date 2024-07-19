package app.bpartners.geojobs.endpoint.rest.security.authorizer;

import static app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.rest.security.AuthProvider;
import app.bpartners.geojobs.endpoint.rest.security.model.Authority;
import app.bpartners.geojobs.endpoint.rest.security.model.Principal;
import app.bpartners.geojobs.model.exception.ForbiddenException;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.CommunityZone;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class CommunityZoneAuthorizerIT extends FacadeIT {
  private final MockedStatic<AuthProvider> authProvider = mockStatic(AuthProvider.class);
  @MockBean CommunityAuthorizationRepository caRepository;
  @Autowired CommunityZoneAuthorizer communityZoneAuthorizer;

  @Test
  void should_accept_directly_admin_key() {
    useRole(ROLE_ADMIN);
    assertDoesNotThrow(() -> communityZoneAuthorizer.accept("zoneName"));
  }

  @Test
  void should_throws_if_not_authorized_zone() {
    useRole(ROLE_COMMUNITY);
    when(caRepository.findByApiKey(any()))
        .thenReturn(asOptionalCommunityAuthorization(List.of("zoneName1")));

    var error =
        assertThrows(
            ForbiddenException.class, () -> communityZoneAuthorizer.accept("not-authorized-zone"));
    assertTrue(error.getMessage().contains("not-authorized-zone"));
  }

  @Test
  void should_accept_if_authorized_zone() {
    useRole(ROLE_COMMUNITY);
    when(caRepository.findByApiKey(any()))
        .thenReturn(asOptionalCommunityAuthorization(List.of("zoneName1", "zoneName2")));

    assertDoesNotThrow(
        () -> {
          communityZoneAuthorizer.accept("zoneName1");
          communityZoneAuthorizer.accept("zoneName2");
        });
  }

  private Optional<CommunityAuthorization> asOptionalCommunityAuthorization(
      List<String> zoneNames) {
    var communityZones =
        zoneNames.stream().map(zoneName -> CommunityZone.builder().name(zoneName).build()).toList();
    return Optional.of(CommunityAuthorization.builder().authorizedZones(communityZones).build());
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
