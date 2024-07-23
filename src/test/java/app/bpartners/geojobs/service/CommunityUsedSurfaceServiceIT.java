package app.bpartners.geojobs.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.CommunityUsedSurfaceRepository;
import app.bpartners.geojobs.repository.model.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.CommunityUsedSurface;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class CommunityUsedSurfaceServiceIT extends FacadeIT {
  private static final double LAST_SURFACE_VALUE = 10;
  private static final String API_KEY = "APIKEY";
  private static final Instant DUMMY_DATE = Instant.parse("2024-07-18T00:00:00Z");

  @Autowired CommunityAuthorizationRepository communityAuthorizationRepository;
  @Autowired CommunityUsedSurfaceRepository communityUsedSurfaceRepository;
  @Autowired CommunityUsedSurfaceService communityUsedSurfaceService;

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
        .id("id")
        .name("communityName")
        .maxSurface(5_000)
        .apiKey(API_KEY)
        .authorizedZones(List.of())
        .usedSurfaces(List.of())
        .detectableObjectTypes(List.of())
        .build();
  }

  private static CommunityUsedSurface communityUsedSurface(double value, Instant usageDatetime) {
    return CommunityUsedSurface.builder()
        .id("id")
        .usedSurface(value)
        .usageDatetime(usageDatetime)
        .communityAuthorization(communityAuthorization())
        .build();
  }

  @Test
  void can_take_last_used_surface() {
    var expectedUsedSurface = communityUsedSurface(LAST_SURFACE_VALUE, DUMMY_DATE);
    var actualUsedSurface =
        communityUsedSurfaceService.getLastUsedSurfaceByCommunityApiKey(API_KEY);
    assertEquals(expectedUsedSurface, actualUsedSurface.orElse(null));

    communityUsedSurfaceRepository.deleteAll();
    assertTrue(communityUsedSurfaceService.getLastUsedSurfaceByCommunityApiKey(API_KEY).isEmpty());
  }

  @Test
  void add_first_new_last_used_surface() {
    communityUsedSurfaceRepository.deleteAll();
    var exceptedUsedSurface = communityUsedSurface(10, Instant.now());
    communityUsedSurfaceService.appendLastUsedSurface(exceptedUsedSurface);
    var actualUsedSurface =
        communityUsedSurfaceService.getLastUsedSurfaceByCommunityApiKey(API_KEY).orElseThrow();

    assertEquals(formatUsageDatetime(exceptedUsedSurface), formatUsageDatetime(actualUsedSurface));
  }

  @Test
  void can_append_new_used_surface_with_last_used_surface() {
    var newUsedSurface = communityUsedSurface(20, DUMMY_DATE.plusSeconds(5_000));
    communityUsedSurfaceService.appendLastUsedSurface(newUsedSurface);

    var exceptedUsedSurface = communityUsedSurface(LAST_SURFACE_VALUE + 20, Instant.now());
    var actualLastUsedSurface =
        communityUsedSurfaceService.getLastUsedSurfaceByCommunityApiKey(API_KEY).orElseThrow();
    assertEquals(
        formatUsageDatetime(exceptedUsedSurface), formatUsageDatetime(actualLastUsedSurface));
  }

  private static CommunityUsedSurface formatUsageDatetime(
      CommunityUsedSurface communityUsedSurface) {
    communityUsedSurface.setUsageDatetime(
        communityUsedSurface.getUsageDatetime().truncatedTo(ChronoUnit.MINUTES));
    return communityUsedSurface;
  }
}
