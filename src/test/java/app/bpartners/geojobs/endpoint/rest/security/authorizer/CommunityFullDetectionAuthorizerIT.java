package app.bpartners.geojobs.endpoint.rest.security.authorizer;

import static app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role.ROLE_ADMIN;
import static app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role.ROLE_COMMUNITY;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.PATHWAY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.rest.model.CreateFullDetection;
import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.endpoint.rest.security.AuthProvider;
import app.bpartners.geojobs.endpoint.rest.security.model.Authority;
import app.bpartners.geojobs.endpoint.rest.security.model.Principal;
import app.bpartners.geojobs.model.exception.ForbiddenException;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorizedZone;
import app.bpartners.geojobs.repository.model.community.CommunityDetectableObjectType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;

class CommunityFullDetectionAuthorizerIT extends FacadeIT {
  private final MockedStatic<AuthProvider> authProvider = mockStatic(AuthProvider.class);
  private final String COMMUNITY_ID = "communityId";
  private final String COMMUNITY_API_KEY = "communityApiKey";
  private final String ADMIN_API_KEY = "the-admin-api-key";

  @Autowired CommunityFullDetectionAuthorizer subject;
  @Autowired CommunityAuthorizationRepository caRepository;

  @BeforeEach
  void setup() {
    caRepository.save(communityAuthorization());
  }

  @Test
  void should_accept_directly_admin_api_key() {
    useRole(ROLE_ADMIN);
    assertDoesNotThrow(
        () -> {
          subject.accept(
              asCreateFullDetection(DetectableObjectType.POOL, List.of(feature2000Surface())));
        });
  }

  @Test
  void should_throws_if_object_type_is_not_authorized() {
    useRole(ROLE_COMMUNITY);
    var error =
        assertThrows(
            ForbiddenException.class,
            () -> {
              subject.accept(
                  asCreateFullDetection(DetectableObjectType.POOL, List.of(feature2000Surface())));
            });
    assertTrue(error.getMessage().contains("POOL"));
  }

  @Test
  void should_throws_if_feature_is_not_covered_by_authorized_zone() {
    useRole(ROLE_COMMUNITY);
    var error =
        assertThrows(
            ForbiddenException.class,
            () -> {
              subject.accept(
                  asCreateFullDetection(
                      DetectableObjectType.PATHWAY, List.of(feature50SurfaceOutside2000())));
            });
    assertTrue(error.getMessage().contains("Some given feature is not allowed"));
  }

  @AfterEach
  void cleanMock() {
    authProvider.close();
  }

  private void useRole(Authority.Role role) {
    var apiKey = role.equals(ROLE_COMMUNITY) ? COMMUNITY_API_KEY : ADMIN_API_KEY;
    var userPrincipal = new Principal(apiKey, Set.of(new Authority(role)));
    authProvider.when(AuthProvider::getPrincipal).thenReturn(userPrincipal);
  }

  private Feature feature1555SurfaceInside2000() {
    Feature feature = new Feature();
    var sideLength = Math.sqrt(1_555);
    var coordinates =
        List.of(
            List.of(
                List.of(BigDecimal.valueOf(2.5), BigDecimal.valueOf(2.5)),
                List.of(BigDecimal.valueOf(2.5), BigDecimal.valueOf(2.5 + sideLength)),
                List.of(BigDecimal.valueOf(2.5 + sideLength), BigDecimal.valueOf(2.5 + sideLength)),
                List.of(BigDecimal.valueOf(2.5 + sideLength), BigDecimal.valueOf(2.5)),
                List.of(BigDecimal.valueOf(2.5), BigDecimal.valueOf(2.5))));
    MultiPolygon multiPolygon = new MultiPolygon().coordinates(List.of(coordinates));
    feature.setGeometry(multiPolygon);
    return feature;
  }

  private Feature feature50SurfaceOutside2000() {
    Feature feature = new Feature();
    var sideLength = Math.sqrt(50);
    var coordinates =
        List.of(
            List.of(
                List.of(BigDecimal.valueOf(40), BigDecimal.valueOf(-5)),
                List.of(BigDecimal.valueOf(40), BigDecimal.valueOf(-5 + sideLength)),
                List.of(BigDecimal.valueOf(40 + sideLength), BigDecimal.valueOf(-5 + sideLength)),
                List.of(BigDecimal.valueOf(40 + sideLength), BigDecimal.valueOf(-5)),
                List.of(BigDecimal.valueOf(40), BigDecimal.valueOf(-5))));
    MultiPolygon multiPolygon = new MultiPolygon().coordinates(List.of(coordinates));
    feature.setGeometry(multiPolygon);
    return feature;
  }

  private Feature feature2000Surface() {
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

  private CreateFullDetection asCreateFullDetection(
      DetectableObjectType type, List<Feature> features) {
    return new CreateFullDetection().objectType(type).features(features);
  }

  private CommunityAuthorizedZone communityAuthorizedZone() {
    return CommunityAuthorizedZone.builder()
        .id("dummyId")
        .name("dummyZoneName")
        .multiPolygon(feature2000Surface().getGeometry())
        .communityAuthorizationId(COMMUNITY_ID)
        .build();
  }

  private CommunityAuthorization communityAuthorization() {
    var communityDetectableType =
        CommunityDetectableObjectType.builder()
            .id("dummyId")
            .type(PATHWAY)
            .communityAuthorizationId(COMMUNITY_ID)
            .build();

    return CommunityAuthorization.builder()
        .id("communityId")
        .name("communityName")
        .maxSurface(1_000)
        .apiKey(COMMUNITY_API_KEY)
        .usedSurfaces(List.of())
        .authorizedZones(List.of(communityAuthorizedZone()))
        .detectableObjectTypes(List.of(communityDetectableType))
        .build();
  }
}
