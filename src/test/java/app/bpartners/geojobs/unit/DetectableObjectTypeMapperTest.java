package app.bpartners.geojobs.unit;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.DetectableObjectTypeMapper;
import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import org.junit.jupiter.api.Test;

class DetectableObjectTypeMapperTest {
  DetectableObjectTypeMapper subject = new DetectableObjectTypeMapper();

  @Test
  void to_domain_ok() {
    assertEquals(TREE, subject.toDomain(DetectableObjectType.TREE));
    assertEquals(ROOF, subject.toDomain(DetectableObjectType.ROOF));
    assertEquals(POOL, subject.toDomain(DetectableObjectType.POOL));
    assertEquals(PATHWAY, subject.toDomain(DetectableObjectType.PATHWAY));
    assertEquals(SOLAR_PANEL, subject.toDomain(DetectableObjectType.SOLAR_PANEL));
  }

  @Test
  void to_domain_ko() {
    assertThrows(
        NotImplementedException.class, () -> subject.toDomain(DetectableObjectType.SIDEWALK));
    assertThrows(NotImplementedException.class, () -> subject.toDomain(DetectableObjectType.LINE));
    assertThrows(
        NotImplementedException.class, () -> subject.toDomain(DetectableObjectType.GREEN_SPACE));
  }

  @Test
  void to_rest_ok() {
    assertEquals(DetectableObjectType.TREE, subject.toRest(TREE));
    assertEquals(DetectableObjectType.ROOF, subject.toRest(ROOF));
    assertEquals(DetectableObjectType.POOL, subject.toRest(POOL));
    assertEquals(DetectableObjectType.PATHWAY, subject.toRest(PATHWAY));
    assertEquals(DetectableObjectType.SOLAR_PANEL, subject.toRest(SOLAR_PANEL));
    assertEquals(DetectableObjectType.LINE, subject.toRest(LINE));
    assertEquals(DetectableObjectType.SIDEWALK, subject.toRest(SIDEWALK));
    assertEquals(DetectableObjectType.GREEN_SPACE, subject.toRest(GREEN_SPACE));
  }
}