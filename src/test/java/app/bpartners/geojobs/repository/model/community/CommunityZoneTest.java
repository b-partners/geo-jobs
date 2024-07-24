package app.bpartners.geojobs.repository.model.community;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CommunityZoneTest {

  @Test
  void getter_setter_test() {
    CommunityZone zone = new CommunityZone();
    zone.setId("zone1");
    zone.setName("Zone1");
    zone.setCommunityAuthorizationId("communityId");

    assertEquals("zone1", zone.getId());
    assertEquals("Zone1", zone.getName());
    assertEquals("communityId", zone.getCommunityAuthorizationId());
  }

  @Test
  void equals_and_hashcode_test() {
    CommunityZone zone1 = new CommunityZone();
    zone1.setId("zone1");
    zone1.setName("Zone1");

    CommunityZone zone2 = new CommunityZone();
    zone2.setId("zone1");
    zone2.setName("Zone1");

    assertEquals(zone1, zone2);
    assertEquals(zone1.hashCode(), zone2.hashCode());
  }
}
