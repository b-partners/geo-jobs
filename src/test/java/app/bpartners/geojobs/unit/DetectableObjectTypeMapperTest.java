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
    assertEquals(ARBRE, subject.toDomain(DetectableObjectType.ARBRE));
    assertEquals(TOITURE_REVETEMENT, subject.toDomain(DetectableObjectType.TOITURE_REVETEMENT));
    assertEquals(PISCINE, subject.toDomain(DetectableObjectType.PISCINE));
    assertEquals(PASSAGE_PIETON, subject.toDomain(DetectableObjectType.PASSAGE_PIETON));
    assertEquals(
        PANNEAU_PHOTOVOLTAIQUE, subject.toDomain(DetectableObjectType.PANNEAU_PHOTOVOLTAIQUE));
  }

  @Test
  void to_domain_ko() {
    assertThrows(
        NotImplementedException.class, () -> subject.toDomain(DetectableObjectType.TROTTOIR));
    assertThrows(NotImplementedException.class, () -> subject.toDomain(DetectableObjectType.LINE));
    assertThrows(
        NotImplementedException.class, () -> subject.toDomain(DetectableObjectType.ESPACE_VERT));
  }

  @Test
  void to_rest_ok() {
    assertEquals(DetectableObjectType.ARBRE, subject.toRest(ARBRE));
    assertEquals(DetectableObjectType.TOITURE_REVETEMENT, subject.toRest(TOITURE_REVETEMENT));
    assertEquals(DetectableObjectType.PISCINE, subject.toRest(PISCINE));
    assertEquals(DetectableObjectType.PASSAGE_PIETON, subject.toRest(PASSAGE_PIETON));
    assertEquals(
        DetectableObjectType.PANNEAU_PHOTOVOLTAIQUE, subject.toRest(PANNEAU_PHOTOVOLTAIQUE));
    assertEquals(DetectableObjectType.LINE, subject.toRest(LINE));
    assertEquals(DetectableObjectType.TROTTOIR, subject.toRest(TROTTOIR));
    assertEquals(DetectableObjectType.ESPACE_VERT, subject.toRest(ESPACE_VERT));
  }
}
