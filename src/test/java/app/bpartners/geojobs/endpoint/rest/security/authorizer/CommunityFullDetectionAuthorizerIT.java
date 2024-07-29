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
import app.bpartners.geojobs.service.CommunityUsedSurfaceService;
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
  private static final String COMMUNITY_ID = "communityId";
  private static final String COMMUNITY_API_KEY = "communityApiKey";
  private static final String ADMIN_API_KEY = "the-admin-api-key";

  @Autowired CommunityFullDetectionAuthorizer subject;
  @Autowired CommunityAuthorizationRepository caRepository;
  @Autowired CommunityUsedSurfaceService communityUsedSurfaceService;

  @BeforeEach
  void setup() {
    caRepository.save(communityAuthorization());
  }

  @AfterEach
  void cleanMock() {
    caRepository.deleteAll();
    authProvider.close();
  }

  @Test
  void should_accept_directly_admin_api_key() {
    useRole(ROLE_ADMIN);
    var createFullDetection =
        asCreateFullDetection(DetectableObjectType.POOL, List.of(feature2000Surface()));

    assertDoesNotThrow(
        () -> {
          subject.accept(createFullDetection);
        });
  }

  @Test
  void should_accept_if_authorization_is_correct() {
    useRole(ROLE_COMMUNITY);
    var createFullDetection =
        asCreateFullDetection(DetectableObjectType.PATHWAY, List.of(feature1550InsideSurface()));

    assertDoesNotThrow(
        () -> {
          subject.accept(createFullDetection);
        });
  }

  @Test
  void should_throws_if_community_does_not_have_authorized_zone() {
    useRole(ROLE_COMMUNITY);
    caRepository.deleteAll();
    var communityAuthorization = communityAuthorization();
    communityAuthorization.setAuthorizedZones(List.of());
    var createFullDetection =
        asCreateFullDetection(DetectableObjectType.PATHWAY, List.of(feature2000Surface()));
    caRepository.save(communityAuthorization);

    var error =
        assertThrows(
            ForbiddenException.class,
            () -> {
              subject.accept(createFullDetection);
            });
    assertTrue(error.getMessage().contains("There is no zone authorized"));
  }

  @Test
  void should_throws_if_object_type_is_not_authorized() {
    useRole(ROLE_COMMUNITY);
    var createFullDetection =
        asCreateFullDetection(DetectableObjectType.POOL, List.of(feature2000Surface()));

    var error =
        assertThrows(
            ForbiddenException.class,
            () -> {
              subject.accept(createFullDetection);
            });
    assertTrue(error.getMessage().contains("POOL"));
  }

  @Test
  void should_throws_if_feature_is_not_covered_by_authorized_zone() {
    useRole(ROLE_COMMUNITY);
    var createFullDetection =
        asCreateFullDetection(DetectableObjectType.PATHWAY, List.of(feature50SurfaceOutside2000()));

    var error =
        assertThrows(
            ForbiddenException.class,
            () -> {
              subject.accept(createFullDetection);
            });
    assertTrue(error.getMessage().contains("Some given feature is not allowed"));
  }

  @Test
  void should_throws_if_max_surface_is_exceeded() {
    useRole(ROLE_COMMUNITY);
    communityUsedSurfaceService.appendLastUsedSurface(COMMUNITY_ID, 500);
    var createFullDetection =
        asCreateFullDetection(DetectableObjectType.PATHWAY, List.of(feature1550InsideSurface()));

    var error =
        assertThrows(
            ForbiddenException.class,
            () -> {
              subject.accept(createFullDetection);
            });
    assertTrue(error.getMessage().contains("Max Surface is exceeded"));
    assertTrue(error.getMessage().contains("2000"));
  }

  private void useRole(Authority.Role role) {
    var apiKey = role.equals(ROLE_COMMUNITY) ? COMMUNITY_API_KEY : ADMIN_API_KEY;
    var userPrincipal = new Principal(apiKey, Set.of(new Authority(role)));
    authProvider.when(AuthProvider::getPrincipal).thenReturn(userPrincipal);
  }

  private Feature feature1550InsideSurface() {
    Feature feature = new Feature();
    BigDecimal sideLength = BigDecimal.valueOf(Math.sqrt(1550));
    var coordinates =
        List.of(
            List.of(
                List.of(BigDecimal.valueOf(2.675), BigDecimal.valueOf(2.675)),
                List.of(BigDecimal.valueOf(2.675), sideLength.add(BigDecimal.valueOf(2.675))),
                List.of(
                    sideLength.add(BigDecimal.valueOf(2.675)),
                    sideLength.add(BigDecimal.valueOf(2.675))),
                List.of(sideLength.add(BigDecimal.valueOf(2.675)), BigDecimal.valueOf(2.675)),
                List.of(BigDecimal.valueOf(2.675), BigDecimal.valueOf(2.675))));
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
        .maxSurface(2_000)
        .apiKey(COMMUNITY_API_KEY)
        .usedSurfaces(List.of())
        .authorizedZones(List.of(communityAuthorizedZone()))
        .detectableObjectTypes(List.of(communityDetectableType))
        .build();
  }
}
