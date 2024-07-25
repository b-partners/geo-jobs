package app.bpartners.geojobs.repository.model.community;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CommunityAuthorizedZoneTest {

  @Test
  void getter_setter_test() {
    CommunityAuthorizedZone zone = new CommunityAuthorizedZone();
    zone.setId("zone1");
    zone.setName("Zone1");
    zone.setCommunityAuthorizationId("communityId");

    assertEquals("zone1", zone.getId());
    assertEquals("Zone1", zone.getName());
    assertEquals("communityId", zone.getCommunityAuthorizationId());
  }

  @Test
  void equals_and_hashcode_test() {
    CommunityAuthorizedZone zone1 = new CommunityAuthorizedZone();
    zone1.setId("zone1");
    zone1.setName("Zone1");

    CommunityAuthorizedZone zone2 = new CommunityAuthorizedZone();
    zone2.setId("zone1");
    zone2.setName("Zone1");

    assertEquals(zone1, zone2);
    assertEquals(zone1.hashCode(), zone2.hashCode());
  }
}
