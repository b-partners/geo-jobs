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
  private static final double LAST_USED_SURFACE_VALUE = 10;
  private static final String LAST_USED_SURFACE_ID = "dummyId";
  private static final String COMMUNITY_AUTHORIZATION_APIKEY = "communityApiKey";
  private static final Instant DUMMY_DATE = Instant.parse("2024-07-18T00:00:00Z");

  @Autowired CommunityAuthorizationRepository communityAuthorizationRepository;
  @Autowired CommunityUsedSurfaceRepository communityUsedSurfaceRepository;
  @Autowired CommunityUsedSurfaceService communityUsedSurfaceService;

  @BeforeEach
  void setup() {
    communityAuthorizationRepository.save(communityAuthorization());
    communityUsedSurfaceRepository.save(
        communityUsedSurface(LAST_USED_SURFACE_ID, LAST_USED_SURFACE_VALUE, DUMMY_DATE));
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
        .apiKey(COMMUNITY_AUTHORIZATION_APIKEY)
        .authorizedZones(List.of())
        .usedSurfaces(List.of())
        .detectableObjectTypes(List.of())
        .build();
  }

  private static CommunityUsedSurface communityUsedSurface(
      String id, double value, Instant usageDatetime) {
    return CommunityUsedSurface.builder()
        .id(id)
        .usedSurface(value)
        .usageDatetime(usageDatetime)
        .communityAuthorization(communityAuthorization())
        .build();
  }

  @Test
  void take_last_used_surface_by_api_key_when_empty_or_not() {
    var exceptedLastUsedSurfaceValue =
        communityUsedSurface(LAST_USED_SURFACE_ID, LAST_USED_SURFACE_VALUE, DUMMY_DATE);
    var actualLastUsedSurface =
        communityUsedSurfaceService.getLastUsedSurfaceByApiKey(COMMUNITY_AUTHORIZATION_APIKEY);
    assertEquals(exceptedLastUsedSurfaceValue, actualLastUsedSurface.orElse(null));

    communityUsedSurfaceRepository.deleteAll();
    assertTrue(
        communityUsedSurfaceService
            .getLastUsedSurfaceByApiKey(COMMUNITY_AUTHORIZATION_APIKEY)
            .isEmpty());
  }

  @Test
  void new_last_used_surface_should_be_last_used_surface_if_empty() {
    communityUsedSurfaceRepository.deleteAll();
    var exceptedLastUsedSurfaceValue = communityUsedSurface("dummy", 10, Instant.now());
    communityUsedSurfaceService.appendLastUsedSurface(exceptedLastUsedSurfaceValue);
    var actualLastUsedSurface =
        communityUsedSurfaceService
            .getLastUsedSurfaceByApiKey(COMMUNITY_AUTHORIZATION_APIKEY)
            .orElseThrow();
    assertEquals(
        formatCommunitySurfaceUsageDatetime(exceptedLastUsedSurfaceValue),
        formatCommunitySurfaceUsageDatetime(actualLastUsedSurface));
  }

  @Test
  void can_append_last_used_surface_when_not_empty() {
    var newLastUsedSurfaceValue = 20;
    var newLastUsedSurface = communityUsedSurface("dummyId2", 20, DUMMY_DATE.plusSeconds(5_000));
    communityUsedSurfaceService.appendLastUsedSurface(newLastUsedSurface);

    var exceptedLastUsedSurfaceValue = LAST_USED_SURFACE_VALUE + newLastUsedSurfaceValue;
    var exceptedLastUsedSurface =
        communityUsedSurface("dummyId2", exceptedLastUsedSurfaceValue, Instant.now());
    var actualLastUsedSurface =
        communityUsedSurfaceService
            .getLastUsedSurfaceByApiKey(COMMUNITY_AUTHORIZATION_APIKEY)
            .orElseThrow();
    assertEquals(
        formatCommunitySurfaceUsageDatetime(exceptedLastUsedSurface),
        formatCommunitySurfaceUsageDatetime(actualLastUsedSurface));
  }

  private static CommunityUsedSurface formatCommunitySurfaceUsageDatetime(
      CommunityUsedSurface communityUsedSurface) {
    communityUsedSurface.setUsageDatetime(
        communityUsedSurface.getUsageDatetime().truncatedTo(ChronoUnit.MINUTES));
    return communityUsedSurface;
  }
}
