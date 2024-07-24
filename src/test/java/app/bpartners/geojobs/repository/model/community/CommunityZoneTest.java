package app.bpartners.geojobs.repository.model.community;

import static org.junit.jupiter.api.Assertions.*;

import app.bpartners.geojobs.repository.model.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.CommunityZone;
import org.junit.jupiter.api.Test;

public class CommunityZoneTest {

  @Test
  public void constructor_test() {
    CommunityZone zone = new CommunityZone();
    zone.setId("zone1");
    zone.setName("Zone 1");
    CommunityAuthorization authorization = new CommunityAuthorization();
    zone.setCommunityAuthorization(authorization);

    assertEquals("zone1", zone.getId());
    assertEquals("Zone 1", zone.getName());
    assertEquals(authorization, zone.getCommunityAuthorization());
  }

  @Test
  public void equals_and_hashcode_test() {
    CommunityZone zone1 = new CommunityZone();
    zone1.setId("zone1");
    zone1.setName("Zone 1");

    CommunityZone zone2 = new CommunityZone();
    zone2.setId("zone1");
    zone2.setName("Zone 1");

    assertEquals(zone1, zone2);
    assertEquals(zone1.hashCode(), zone2.hashCode());
  }

  @Test
  public void to_string_test() {
    CommunityZone zone = new CommunityZone();
    zone.setId("zone1");
    zone.setName("Zone 1");

    String expected = "CommunityZone{id='zone1', name='Zone 1'}";
    assertEquals(expected, zone.toString());
  }
}
