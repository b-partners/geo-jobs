package app.bpartners.geojobs.unit;

import static app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType.*;
import static app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType.ARBRE;
import static app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType.ESPACE_VERT;
import static app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType.PANNEAU_PHOTOVOLTAIQUE;
import static app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType.PARKING;
import static app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType.PASSAGE_PIETON;
import static app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType.PISCINE;
import static app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType.RISQUE_FEU;
import static app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType.TOITURE_REVETEMENT;
import static app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType.TROTTOIR;
import static app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType.VELUX;
import static app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType.VOIE_CARROSSABLE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.LINE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.DetectableObjectTypeMapper;
import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import java.util.List;
import org.junit.jupiter.api.Test;

class DetectableObjectTypeMapperTest {
  DetectableObjectTypeMapper subject = new DetectableObjectTypeMapper();

  @Test
  void to_domain_ok() {
    assertEquals(DetectableType.ARBRE, subject.toDomain(ARBRE));
    assertEquals(DetectableType.TOITURE_REVETEMENT, subject.toDomain(TOITURE_REVETEMENT));
    assertEquals(DetectableType.PISCINE, subject.toDomain(PISCINE));
    assertEquals(DetectableType.PASSAGE_PIETON, subject.toDomain(PASSAGE_PIETON));
    assertEquals(DetectableType.PANNEAU_PHOTOVOLTAIQUE, subject.toDomain(PANNEAU_PHOTOVOLTAIQUE));
  }

  @Test
  void to_rest_ok() {
    assertEquals(ARBRE, subject.toRest(DetectableType.ARBRE));
    assertEquals(TOITURE_REVETEMENT, subject.toRest(DetectableType.TOITURE_REVETEMENT));
    assertEquals(PISCINE, subject.toRest(DetectableType.PISCINE));
    assertEquals(PASSAGE_PIETON, subject.toRest(DetectableType.PASSAGE_PIETON));
    assertEquals(PANNEAU_PHOTOVOLTAIQUE, subject.toRest(DetectableType.PANNEAU_PHOTOVOLTAIQUE));
    assertEquals(DetectableObjectType.LINE, subject.toRest(LINE));
    assertEquals(TROTTOIR, subject.toRest(DetectableType.TROTTOIR));
    assertEquals(ESPACE_VERT, subject.toRest(DetectableType.ESPACE_VERT));
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
            DetectableObjectType.MOISISSURE,
            DetectableObjectType.USURE,
            DetectableObjectType.FISSURE_CASSURE,
            DetectableObjectType.OBSTACLE,
            DetectableObjectType.CHEMINEE,
            DetectableObjectType.HUMIDITE,
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
    var object = new BPConformitePluModel();

    var actual = subject.mapFromModel(object);

    var expected =
        List.of(TOITURE_REVETEMENT, ARBRE, VELUX, PANNEAU_PHOTOVOLTAIQUE, ESPACE_VERT, PISCINE);
    assertEquals(expected, actual);
  }

  @Test
  void map_from_model_BP_Climat_Resilience_Model() {
    var object = new BPClimatResilienceModel();

    var actual = subject.mapFromModel(object);

    var expected = List.of(PARKING, PANNEAU_PHOTOVOLTAIQUE, ARBRE, ESPACE_VERT);
    assertEquals(expected, actual);
  }

  @Test
  void map_from_modl_BP_Trottoirs_Model() {
    var object = new BPTrottoirsModel();

    var actual = subject.mapFromModel(object);

    var expected = List.of(TROTTOIR, VOIE_CARROSSABLE, ARBRE, ESPACE_VERT_PARKING);
    assertEquals(expected, actual);
  }

  @Test
  void map_from_modl_BP_Old_Model() {
    var object = new BPOldModel();

    var actual = subject.mapFromModel(object);

    var expected =
        List.of(
            ARBRE,
            ESPACE_VERT,
            TOITURE_REVETEMENT,
            VOIE_CARROSSABLE,
            TROTTOIR,
            PARKING,
            RISQUE_FEU);
    assertEquals(expected, actual);
  }
}
