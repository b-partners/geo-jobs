package app.bpartners.geojobs.unit;

import static app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType.ARBRE;
import static app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType.ESPACE_VERT;
import static app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType.PANNEAU_PHOTOVOLTAIQUE;
import static app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType.PARKING;
import static app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType.PASSAGE_PIETON;
import static app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType.PISCINE;
import static app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType.TOITURE_REVETEMENT;
import static app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType.TROTTOIR;
import static app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType.VELUX;
import static app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType.VOIE_CARROSSABLE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.DetectableObjectTypeMapper;
import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import java.util.List;
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
    assertEquals(DetectableObjectType.ARBRE, subject.toRest(DetectableType.ARBRE));
    assertEquals(
        DetectableObjectType.TOITURE_REVETEMENT, subject.toRest(DetectableType.TOITURE_REVETEMENT));
    assertEquals(DetectableObjectType.PISCINE, subject.toRest(PISCINE));
    assertEquals(
        DetectableObjectType.PASSAGE_PIETON, subject.toRest(DetectableType.PASSAGE_PIETON));
    assertEquals(
        DetectableObjectType.PANNEAU_PHOTOVOLTAIQUE,
        subject.toRest(DetectableType.PANNEAU_PHOTOVOLTAIQUE));
    assertEquals(DetectableObjectType.LINE, subject.toRest(LINE));
    assertEquals(DetectableObjectType.TROTTOIR, subject.toRest(DetectableType.TROTTOIR));
    assertEquals(DetectableObjectType.ESPACE_VERT, subject.toRest(DetectableType.ESPACE_VERT));
  }

  @Test
  void map_from_model_BP_Toiture_Model() {
    var object = new BPToitureModel();

    var actual = subject.mapFromModel(object);

    var expected =
        List.of(
            ARBRE,
            TOITURE_REVETEMENT,
            PANNEAU_PHOTOVOLTAIQUE,
            MOISISSURE,
            USURE,
            FISSURE_CASSURE,
            OBSTACLE,
            CHEMINEE,
            HUMIDITE,
            RISQUE_FEU);
    assertEquals(expected, actual);
  }

  @Test
  void map_from_model_BP_Lom_Model() {
    var object = new BPLomModel();

    var actual = subject.mapFromModel(object);

    var expected = List.of(PASSAGE_PIETON, TROTTOIR, VOIE_CARROSSABLE);
    assertEquals(expected, actual);
  }

  @Test
  void map_from_model_BP_Zan_Model() {
    var object = new BPZanModel();

    var actual = subject.mapFromModel(object);

    var expected =
        List.of(ARBRE, ESPACE_VERT, TOITURE_REVETEMENT, VOIE_CARROSSABLE, TROTTOIR, PARKING);
    assertEquals(expected, actual);
  }

  @Test
  void map_from_model_BP_Conformite_Plu_Model() {
    var object = new BPConformitePlu();

    var actual = subject.mapFromModel(object);

    var expected =
        List.of(TOITURE_REVETEMENT, ARBRE, VELUX, PANNEAU_PHOTOVOLTAIQUE, ESPACE_VERT, PISCINE);
    assertEquals(expected, actual);
  }

  @Test
  void map_from_model_BP_Climat_Resilience_Model() {
    var object = new BPClimatREsilience();

    var actual = subject.mapFromModel(object);

    var expected = List.of(PARKING, PANNEAU_PHOTOVOLTAIQUE, ARBRE, ESPACE_VERT);
    assertEquals(expected, actual);
  }
}
