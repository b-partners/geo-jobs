package app.bpartners.geojobs.repository.model.community;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.repository.model.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.CommunityDetectableObjectType;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import org.junit.jupiter.api.Test;

class CommunityDetectableObjectTypeTest {
  @Test
  public void constructor_test() {
    CommunityDetectableObjectType value = new CommunityDetectableObjectType();
    value.setId("123");
    value.setType(DetectableType.POOL);
    CommunityAuthorization authorization = new CommunityAuthorization();
    value.setCommunityAuthorization(authorization);

    assertEquals("123", value.getId());
    assertEquals(DetectableType.POOL, value.getType());
    assertEquals(authorization, value.getCommunityAuthorization());
  }

  @Test
  public void equals_and_hashcode_test() {
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
