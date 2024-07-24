package app.bpartners.geojobs.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.repository.model.CommunityAuthorization;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CommunityAuthorizationRepositoryIT extends FacadeIT {
  @Autowired CommunityAuthorizationRepository communityAuthorizationRepository;
  private static String APIKEY = "APIKEY";

  @BeforeEach
  void setup() {
    communityAuthorizationRepository.save(communityAuthorization());
  }

  @Test
  void can_get_community_by_api_key() {
    var excepted = communityAuthorization();

    var actual = communityAuthorizationRepository.findByApiKey(APIKEY);

    assertEquals(excepted, actual.orElse(null));
  }

  static CommunityAuthorization communityAuthorization() {
    return CommunityAuthorization.builder()
        .id("dummyId")
        .apiKey(APIKEY)
        .name("Community name")
        .maxSurface(5_000)
        .authorizedZones(List.of())
        .detectableObjectTypes(List.of())
        .usedSurfaces(List.of())
        .build();
  }
}
