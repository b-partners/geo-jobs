package app.bpartners.geojobs.endpoint.rest.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.DetectableObjectTypeMapper;
import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectModel;
import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType;
import app.bpartners.geojobs.endpoint.rest.model.ModelName;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class DetectableObjectTypeMapperTest {
  private static final Set<DetectableType> TYPES_WITHOUT_REST_EQUIVALENT =
      Set.of(DetectableType.TOMBE, DetectableType.POINTILLE, DetectableType.NOT_BUILDING);

  private static final Map<DetectableType, Double> EXPECTED_MIN_CONFIDENCE =
      Map.ofEntries(
          Map.entry(DetectableType.TROTTOIR, 0.252),
          Map.entry(DetectableType.PISCINE, 0.27),
          Map.entry(DetectableType.PANNEAU_PHOTOVOLTAIQUE, 0.27),
          Map.entry(DetectableType.TOITURE_REVETEMENT, 0.252),
          Map.entry(DetectableType.LINE, 0.252),
          Map.entry(DetectableType.BATI_TUILES, 0.252),
          Map.entry(DetectableType.PARKING, 0.252),
          Map.entry(DetectableType.BATI_BETON, 0.252),
          Map.entry(DetectableType.BATI_AUTRES, 0.252),
          Map.entry(DetectableType.ARBRE, 0.2504),
          Map.entry(DetectableType.PASSAGE_PIETON, 0.29),
          Map.entry(DetectableType.ESPACE_VERT, 0.251),
          Map.entry(DetectableType.BATI_ARDOISE, 0.255),
          Map.entry(DetectableType.VOIE_CARROSSABLE, 0.0),
          Map.entry(DetectableType.MOISISSURE_CLAIR, 0.0),
          Map.entry(DetectableType.MOISISSURE_COULEUR, 0.0),
          Map.entry(DetectableType.MOISISSURE_NOIRCIE, 0.0),
          Map.entry(DetectableType.USURE_IMPORTANTE, 0.0),
          Map.entry(DetectableType.USURE_LEGER, 0.0),
          Map.entry(DetectableType.FISSURE_CASSURE, 0.0),
          Map.entry(DetectableType.OBSTACLE, 0.0),
          Map.entry(DetectableType.CHEMINEE, 0.0),
          Map.entry(DetectableType.LUMINEUX, 0.0),
          Map.entry(DetectableType.HUMIDITE_CLAIR, 0.0),
          Map.entry(DetectableType.HUMIDITE_INTENSE, 0.0),
          Map.entry(DetectableType.RISQUE_FEU, 0.0),
          Map.entry(DetectableType.VELUX, 0.0),
          Map.entry(DetectableType.ESPACE_VERT_PARKING, 0.0));

  DetectableObjectTypeMapper subject = new DetectableObjectTypeMapper();

  @ParameterizedTest
  @EnumSource(DetectableObjectType.class)
  void every_rest_type_maps_to_the_domain_type_of_the_same_name(DetectableObjectType rest) {
    var actual = subject.toDomain(rest);

    assertEquals(rest.name(), actual.name());
  }

  @ParameterizedTest
  @EnumSource(DetectableType.class)
  void every_domain_type_maps_to_the_rest_type_of_the_same_name_when_there_is_one(
      DetectableType domain) {
    var actual = subject.toRest(domain);

    if (TYPES_WITHOUT_REST_EQUIVALENT.contains(domain)) {
      assertNull(actual);
    } else {
      assertEquals(domain.name(), actual.name());
    }
  }

  @ParameterizedTest
  @EnumSource(ModelName.class)
  void every_model_is_mapped_to_object_types(ModelName modelName) {
    var actual = subject.mapFromModel(modelName);

    assertNotNull(actual);
  }

  @Test
  void model_without_name_gives_no_object_type() {
    var actual = subject.mapFromModel(new DetectableObjectModel());

    assertEquals(List.of(), actual);
  }

  @Test
  void model_with_name_gives_the_object_types_of_that_model() {
    var actual = subject.mapFromModel(new DetectableObjectModel().modelName(ModelName.TOITURE));

    assertEquals(DetectableObjectTypeMapper.detectableObjectTypeForToitureModel(), actual);
  }

  @ParameterizedTest
  @EnumSource(ModelName.class)
  void default_configuration_uses_the_minimum_confidence_expected_for_each_object_type(
      ModelName modelName) {
    var actual = subject.mapDefaultConfigurationsFromModel("detectionId", modelName);

    actual.forEach(
        configuration ->
            assertEquals(
                EXPECTED_MIN_CONFIDENCE.getOrDefault(
                    configuration.getObjectType(), DetectableObjectTypeMapper.DEFAULT_CONFIDENCE),
                configuration.getMinConfidenceForDetection(),
                "min confidence of " + configuration.getObjectType()));
  }

  @Test
  void default_configurations_of_conformity_model_use_piscine_threshold() {
    var actual = subject.mapDefaultConfigurationsFromModel("detectionId", ModelName.CONFIRMITE_PLU);

    var piscine =
        actual.stream()
            .filter(configuration -> configuration.getObjectType() == DetectableType.PISCINE)
            .findFirst()
            .orElseThrow();
    assertEquals(0.27, piscine.getMinConfidenceForDetection());
  }

  @Test
  void default_configurations_carry_detection_id_and_no_job_nor_bucket() {
    var actual = subject.mapDefaultConfigurationsFromModel("detectionId", ModelName.TOITURE);

    assertEquals(subject.mapFromModel(ModelName.TOITURE).size(), actual.size());
    actual.forEach(
        configuration -> {
          assertEquals("detectionId", configuration.getDetectionId());
          assertNull(configuration.getDetectionJobId());
          assertNull(configuration.getBucketStorageName());
        });
  }
}
