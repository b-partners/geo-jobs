package app.bpartners.geojobs.repository.impl;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.POOL;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.ROOF;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.model.CommunityAuthorizationDetails;
import app.bpartners.geojobs.model.exception.ForbiddenException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class CommunityAuthorizationDetailsRepositoryImplIT extends FacadeIT {
  @Autowired CommunityAuthorizationDetailsRepositoryImpl communityAuthorizationDetailsRepository;

  @Test
  void should_throws_ForbiddenException_if_unknown_community_keys() {
    assertThrows(
        ForbiddenException.class,
        () -> communityAuthorizationDetailsRepository.findByApiKey("unknown_api_key"));
  }

  @Test
  void can_take_correct_community_with_api_keys() {
    var expectedCommunity1AuthDetails =
        CommunityAuthorizationDetails.builder()
            .id("community1_id")
            .communityName("community1_name")
            .apiKey("community1_key")
            .authorizedZoneNames(List.of("zoneName1"))
            .maxSurface(5_000)
            .detectableObjectTypes(List.of(ROOF, POOL))
            .build();
    var community1AuthDetails =
        communityAuthorizationDetailsRepository.findByApiKey("community1_key");
    assertEquals(expectedCommunity1AuthDetails, community1AuthDetails);
  }
}
