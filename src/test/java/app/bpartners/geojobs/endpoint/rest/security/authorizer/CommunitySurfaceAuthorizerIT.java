package app.bpartners.geojobs.endpoint.rest.security.authorizer;

import static app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role.ROLE_ADMIN;
import static app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role.ROLE_COMMUNITY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.endpoint.rest.security.AuthProvider;
import app.bpartners.geojobs.endpoint.rest.security.model.Authority;
import app.bpartners.geojobs.endpoint.rest.security.model.Principal;
import app.bpartners.geojobs.model.exception.ForbiddenException;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.CommunityUsedSurface;
import app.bpartners.geojobs.service.CommunityUsedSurfaceService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

public class CommunitySurfaceAuthorizerIT extends FacadeIT {
  private final MockedStatic<AuthProvider> authProvider = mockStatic(AuthProvider.class);
  @Autowired CommunitySurfaceAuthorizer communitySurfaceAuthorizer;
  @MockBean CommunityUsedSurfaceService communityUsedSurfaceService;
  @MockBean CommunityAuthorizationRepository communityAuthorizationRepository;

  @BeforeEach
  void setup() {
    when(communityAuthorizationRepository.findByApiKey(any()))
        .thenReturn(Optional.of(CommunityAuthorization.builder().maxSurface(7_000).build()));
  }

  @Test
  void should_accept_directly_admin_key() {
    useRole(ROLE_ADMIN);
    assertDoesNotThrow(() -> communitySurfaceAuthorizer.accept(List.of(oneFeature())));
  }

  @Test
  void should_throw_if_max_surface_is_exceeded_for_community() {
    useRole(ROLE_COMMUNITY);
    when(communityUsedSurfaceService.getLastUsedSurfaceByApiKey(any()))
        .thenReturn(Optional.of(CommunityUsedSurface.builder().usedSurface(3_000).build()));

    var error =
        assertThrows(
            ForbiddenException.class,
            () -> communitySurfaceAuthorizer.accept(List.of(oneFeature())));
    assertTrue(error.getMessage().contains("7000"));
  }

  @Test
  void should_accept_community_if_max_surface_is_not_exceeded() {
    useRole(ROLE_COMMUNITY);
    assertDoesNotThrow(() -> communitySurfaceAuthorizer.accept(List.of(oneFeature())));
  }

  @AfterEach
  void cleanMock() {
    authProvider.close();
  }

  void useRole(Authority.Role role) {
    var userPrincipal = new Principal("dummy-api-key", Set.of(new Authority(role)));
    authProvider.when(AuthProvider::getPrincipal).thenReturn(userPrincipal);
  }

  private static Feature oneFeature() {
    Feature feature = new Feature();
    var coordinates =
        List.of(
            List.of(
                List.of(
                    List.of(BigDecimal.valueOf(48.05622828269508), BigDecimal.valueOf(0)),
                    List.of(
                        BigDecimal.valueOf(24.028114141347547),
                        BigDecimal.valueOf(41.617914502878165)),
                    List.of(
                        BigDecimal.valueOf(-24.028114141347547),
                        BigDecimal.valueOf(41.617914502878165)),
                    List.of(
                        BigDecimal.valueOf(-48.05622828269508),
                        BigDecimal.valueOf(5.8851906145497036E-15)),
                    List.of(
                        BigDecimal.valueOf(-24.02811414134756),
                        BigDecimal.valueOf(-41.61791450287816)),
                    List.of(
                        BigDecimal.valueOf(24.02811414134751),
                        BigDecimal.valueOf(-41.617914502878186)),
                    List.of(BigDecimal.valueOf(48.05622828269508), BigDecimal.valueOf(0)))));
    MultiPolygon multiPolygon = new MultiPolygon().coordinates(coordinates);
    feature.setGeometry(multiPolygon);
    return feature;
  }
}
