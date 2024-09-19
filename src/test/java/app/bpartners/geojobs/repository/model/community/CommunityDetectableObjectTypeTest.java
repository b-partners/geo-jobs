package app.bpartners.geojobs.repository.model.community;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.repository.model.detection.DetectableType;
import org.junit.jupiter.api.Test;

class CommunityDetectableObjectTypeTest {
  @Test
  void getter_setter_test() {
    CommunityDetectableObjectType value = new CommunityDetectableObjectType();
    value.setId("123");
    value.setType(DetectableType.PISCINE);
    value.setCommunityAuthorizationId("communityId");

    assertEquals("123", value.getId());
    assertEquals("communityId", value.getCommunityAuthorizationId());
    assertEquals(DetectableType.PISCINE, value.getType());
  }

  @Test
  void equals_and_hashcode_test() {
    CommunityDetectableObjectType lhsValue = new CommunityDetectableObjectType();
    lhsValue.setId("123");
    lhsValue.setType(DetectableType.LINE);

    CommunityDetectableObjectType rhsValue = new CommunityDetectableObjectType();
    rhsValue.setId("123");
    rhsValue.setType(DetectableType.LINE);

    assertEquals(lhsValue, rhsValue);
    assertEquals(lhsValue.hashCode(), rhsValue.hashCode());
  }
}
