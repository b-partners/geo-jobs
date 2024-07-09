package app.bpartners.geojobs.endpoint.rest.security.authorizer;

import static app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role.ROLE_ADMIN;
import static app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role.ROLE_COMMUNITY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.rest.model.CreateZoneTilingJob;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.endpoint.rest.security.AuthProvider;
import app.bpartners.geojobs.endpoint.rest.security.model.Authority;
import app.bpartners.geojobs.endpoint.rest.security.model.Principal;
import app.bpartners.geojobs.model.CommunityAuthorizationDetails;
import app.bpartners.geojobs.model.exception.ForbiddenException;
import app.bpartners.geojobs.repository.impl.CommunityAuthorizationDetailsRepositoryImpl;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

public class CommunityZoneTilingJobProcessAuthorizerIT extends FacadeIT {
  private final MockedStatic<AuthProvider> authProvider = mockStatic(AuthProvider.class);
  @MockBean CommunityAuthorizationDetailsRepositoryImpl communityAuthorizationDetailsRepository;
  @Autowired CommunityZoneTilingJobProcessAuthorizer communityZoneTilingJobProcessAuthorizer;

  @Test
  void should_accept_directly_admin_key() {
    useRole(ROLE_ADMIN);
    assertDoesNotThrow(
        () -> communityZoneTilingJobProcessAuthorizer.accept(asZoneTilingJob("dummy_zone_name")));
  }

  @Test
  void should_throws_if_not_authorized_zone_names() {
    useRole(ROLE_COMMUNITY);
    when(communityAuthorizationDetailsRepository.findByApiKey(any()))
        .thenReturn(
            asCommunityAuthorizationDetails(List.of("community_zone1", "community_zone2"), 5_000));

    var error =
        assertThrows(
            ForbiddenException.class,
            () ->
                communityZoneTilingJobProcessAuthorizer.accept(
                    asZoneTilingJob("private_zone_name")));
    assertTrue(error.getMessage().contains("private_zone_name"));
  }

  @Test
  void should_throws_if_not_authorized_total_surface() {
    useRole(ROLE_COMMUNITY);
    when(communityAuthorizationDetailsRepository.findByApiKey(any()))
        .thenReturn(
            asCommunityAuthorizationDetails(List.of("community_zone1", "community_zone2"), 0));
    var error =
        assertThrows(
            ForbiddenException.class,
            () ->
                communityZoneTilingJobProcessAuthorizer.accept(asZoneTilingJob("community_zone1")));
    assertTrue(error.getMessage().contains("max allowed surface: 0"));
  }

  @Test
  void should_accept_if_authorization_matched() {
    useRole(ROLE_COMMUNITY);
    when(communityAuthorizationDetailsRepository.findByApiKey(any()))
        .thenReturn(
            asCommunityAuthorizationDetails(
                List.of("community_zone1", "community_zone2"), 5_000_000));

    assertDoesNotThrow(
        () -> {
          communityZoneTilingJobProcessAuthorizer.accept(asZoneTilingJob("community_zone1"));
          communityZoneTilingJobProcessAuthorizer.accept(asZoneTilingJob("community_zone2"));
        });
  }

  private CommunityAuthorizationDetails asCommunityAuthorizationDetails(
      List<String> detectableZoneName, double maxSurface) {
    return new CommunityAuthorizationDetails(
        "dummy_id", "dummy_name", "dummy_name", maxSurface, detectableZoneName, List.of());
  }

  private CreateZoneTilingJob asZoneTilingJob(String zoneName) {
    return new CreateZoneTilingJob().zoneName(zoneName).features(List.of(oneFeature()));
  }

  private void useRole(Authority.Role role) {
    var userPrincipal = new Principal("dummy-api-key", Set.of(new Authority(role)));
    authProvider.when(AuthProvider::getPrincipal).thenReturn(userPrincipal);
  }

  private static Feature oneFeature() {
    Feature feature = new Feature();
    var coordinates =
        List.of(
            List.of(
                List.of(
                    List.of(
                        BigDecimal.valueOf(6.958009303660302),
                        BigDecimal.valueOf(43.543013820437459)),
                    List.of(
                        BigDecimal.valueOf(6.957965493371299),
                        BigDecimal.valueOf(43.543002082885863)),
                    List.of(
                        BigDecimal.valueOf(6.957822106008073),
                        BigDecimal.valueOf(43.543033084979541)),
                    List.of(
                        BigDecimal.valueOf(6.957796040201745),
                        BigDecimal.valueOf(43.543066366941567)),
                    List.of(
                        BigDecimal.valueOf(6.957877191721906),
                        BigDecimal.valueOf(43.543303862183095)),
                    List.of(
                        BigDecimal.valueOf(6.957988034043352),
                        BigDecimal.valueOf(43.54328420602328)),
                    List.of(
                        BigDecimal.valueOf(6.958082768541455),
                        BigDecimal.valueOf(43.543132354704881)),
                    List.of(
                        BigDecimal.valueOf(6.958009303660302),
                        BigDecimal.valueOf(43.543013820437459)))));
    MultiPolygon multiPolygon = new MultiPolygon().coordinates(coordinates);
    feature.setGeometry(multiPolygon);
    return feature;
  }

  @AfterEach
  void cleanMock() {
    authProvider.close();
  }
}
