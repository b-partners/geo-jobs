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
    assertEquals(TOITURE_REVETEMENT, subject.toDomain(DetectableObjectType.TOITURE_REVETEMENT));
    assertEquals(POOL, subject.toDomain(DetectableObjectType.POOL));
    assertEquals(PATHWAY, subject.toDomain(DetectableObjectType.PATHWAY));
    assertEquals(
        PANNEAU_PHOTOVOLTAIQUE, subject.toDomain(DetectableObjectType.PANNEAU_PHOTOVOLTAIQUE));
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
    assertEquals(DetectableObjectType.TOITURE_REVETEMENT, subject.toRest(TOITURE_REVETEMENT));
    assertEquals(DetectableObjectType.POOL, subject.toRest(POOL));
    assertEquals(DetectableObjectType.PATHWAY, subject.toRest(PATHWAY));
    assertEquals(
        DetectableObjectType.PANNEAU_PHOTOVOLTAIQUE, subject.toRest(PANNEAU_PHOTOVOLTAIQUE));
    assertEquals(DetectableObjectType.LINE, subject.toRest(LINE));
    assertEquals(DetectableObjectType.SIDEWALK, subject.toRest(SIDEWALK));
    assertEquals(DetectableObjectType.GREEN_SPACE, subject.toRest(GREEN_SPACE));
  }
}
