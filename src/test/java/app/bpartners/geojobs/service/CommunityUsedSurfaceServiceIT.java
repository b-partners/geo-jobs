package app.bpartners.geojobs.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.CommunityUsedSurfaceRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorizedZone;
import app.bpartners.geojobs.repository.model.community.CommunityUsedSurface;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CommunityUsedSurfaceServiceIT extends FacadeIT {
  private static final double LAST_SURFACE_VALUE = 10;
  private static final String COMMUNITY_ID = "DUMMY_ID";
  private static final Instant DUMMY_DATE = Instant.parse("2024-07-18T00:00:00Z");

  @Autowired CommunityAuthorizationRepository communityAuthorizationRepository;
  @Autowired CommunityUsedSurfaceRepository communityUsedSurfaceRepository;
  @Autowired CommunityUsedSurfaceService subject;

  @BeforeEach
  void setup() {
    communityAuthorizationRepository.save(communityAuthorization());
    communityUsedSurfaceRepository.save(communityUsedSurface(LAST_SURFACE_VALUE, DUMMY_DATE));
  }

  @AfterEach
  void cleanup() {
    communityUsedSurfaceRepository.deleteAll();
    communityAuthorizationRepository.deleteAll();
  }

  private static CommunityAuthorization communityAuthorization() {
    return CommunityAuthorization.builder()
        .id(COMMUNITY_ID)
        .maxSurface(5_000)
        .apiKey("dummmyId")
        .name("communityName")
        .authorizedZones(List.of(communityAuthorizedZone()))
        .usedSurfaces(List.of())
        .detectableObjectTypes(List.of())
        .build();
  }

  private static CommunityUsedSurface communityUsedSurface(double value, Instant usageDatetime) {
    return CommunityUsedSurface.builder()
        .id("id")
        .communityAuthorizationId(COMMUNITY_ID)
        .usedSurface(value)
        .usageDatetime(usageDatetime)
        .build();
  }

  @Test
  void can_take_last_used_surface() {
    var expectedUsedSurface = communityUsedSurface(LAST_SURFACE_VALUE, DUMMY_DATE);

    var actualUsedSurface = subject.getLastUsedSurfaceByCommunityId(COMMUNITY_ID).orElseThrow();

    assertEquals(formatUsedSurface(expectedUsedSurface), formatUsedSurface(actualUsedSurface));
    communityUsedSurfaceRepository.deleteAll();
    assertTrue(subject.getLastUsedSurfaceByCommunityId(COMMUNITY_ID).isEmpty());
  }

  @Test
  void can_append_new_used_surface_with_last_used_surface() {
    var exceptedUsedSurface = communityUsedSurface(LAST_SURFACE_VALUE + 20, Instant.now());

    subject.appendLastUsedSurface(COMMUNITY_ID, 20);
    var actualLastUsedSurface = subject.getLastUsedSurfaceByCommunityId(COMMUNITY_ID).orElseThrow();

    assertEquals(formatUsedSurface(exceptedUsedSurface), formatUsedSurface(actualLastUsedSurface));
  }

  @Test
  void add_first_new_last_used_surface() {
    communityUsedSurfaceRepository.deleteAll();
    var exceptedUsedSurface = communityUsedSurface(15, Instant.now());

    subject.appendLastUsedSurface(COMMUNITY_ID, 15);
    var actualUsedSurface = subject.getLastUsedSurfaceByCommunityId(COMMUNITY_ID).orElseThrow();

    assertEquals(formatUsedSurface(exceptedUsedSurface), formatUsedSurface(actualUsedSurface));
  }

  private static CommunityUsedSurface formatUsedSurface(CommunityUsedSurface communityUsedSurface) {
    communityUsedSurface.setUsageDatetime(
        communityUsedSurface.getUsageDatetime().truncatedTo(ChronoUnit.MINUTES));
    communityUsedSurface.setId("id");
    return communityUsedSurface;
  }

  private static CommunityAuthorizedZone communityAuthorizedZone() {
    return CommunityAuthorizedZone.builder()
        .id("dummyId")
        .name("dummyZoneName")
        .communityAuthorizationId(COMMUNITY_ID)
        .multiPolygon(multiPolygon())
        .build();
  }

  private static MultiPolygon multiPolygon() {
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
    return new MultiPolygon().coordinates(coordinates);
  }
}