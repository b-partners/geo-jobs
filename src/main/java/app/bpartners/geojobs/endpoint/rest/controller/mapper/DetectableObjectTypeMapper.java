package app.bpartners.geojobs.endpoint.rest.controller.mapper;

import static app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType.*;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DetectableObjectTypeMapper {

  public static final double DEFAULT_CONFIDENCE = 1.0;

  public DetectableType toDomain(DetectableObjectType rest) {
    return switch (rest) {
      case PISCINE -> DetectableType.PISCINE;
      case TOITURE_REVETEMENT -> DetectableType.TOITURE_REVETEMENT;
      case ARBRE -> DetectableType.ARBRE;
      case PASSAGE_PIETON -> DetectableType.PASSAGE_PIETON;
      case PANNEAU_PHOTOVOLTAIQUE -> DetectableType.PANNEAU_PHOTOVOLTAIQUE;
      case TROTTOIR -> DetectableType.TROTTOIR;
      case LINE -> DetectableType.LINE;
      case ESPACE_VERT -> DetectableType.ESPACE_VERT;
      case VOIE_CARROSSABLE -> DetectableType.VOIE_CARROSSABLE;
      case MOISISSURE -> DetectableType.MOISISSURE;
      case USURE -> DetectableType.USURE;
      case FISSURE_CASSURE -> DetectableType.FISSURE_CASSURE;
      case OBSTACLE -> DetectableType.OBSTACLE;
      case CHEMINEE -> DetectableType.CHEMINEE;
      case HUMIDITE -> DetectableType.HUMIDITE;
      case RISQUE_FEU -> DetectableType.RISQUE_FEU;
      case VELUX -> DetectableType.VELUX;
      case PARKING -> DetectableType.PARKING;
      case ESPACE_VERT_PARKING -> DetectableType.ESPACE_VERT_PARKING;
      default -> throw new NotImplementedException("Unknown detectable object type " + rest);
    };
  }

  public DetectableObjectType toRest(DetectableType domain) {
    return switch (domain) {
      case PISCINE -> PISCINE;
      case TOITURE_REVETEMENT -> TOITURE_REVETEMENT;
      case ARBRE -> ARBRE;
      case PASSAGE_PIETON -> PASSAGE_PIETON;
      case PANNEAU_PHOTOVOLTAIQUE -> PANNEAU_PHOTOVOLTAIQUE;
      case TROTTOIR -> TROTTOIR;
      case LINE -> LINE;
      case ESPACE_VERT -> ESPACE_VERT;
      case VOIE_CARROSSABLE -> VOIE_CARROSSABLE;
      case MOISISSURE -> MOISISSURE;
      case USURE -> USURE;
      case FISSURE_CASSURE -> FISSURE_CASSURE;
      case OBSTACLE -> OBSTACLE;
      case CHEMINEE -> CHEMINEE;
      case HUMIDITE -> HUMIDITE;
      case RISQUE_FEU -> RISQUE_FEU;
      case VELUX -> VELUX;
      case BATI_TUILES -> BATI_TUILES;
      case PARKING -> PARKING;
      case BATI_BETON -> BATI_BETON;
      case BATI_AUTRES -> BATI_AUTRES;
      case BATI_ARDOISE -> BATI_ARDOISE;
      case ESPACE_VERT_PARKING -> ESPACE_VERT_PARKING;
    };
  }

  public List<DetectableObjectType> mapFromModel(DetectableObjectModel model) {
    var modelName = model.getModelName();
    return modelName == null ? List.of() : mapFromModel(modelName);
  }

  public List<DetectableObjectType> mapFromModel(ModelName modelName) {
    List<DetectableObjectType> objectTypes = new ArrayList<>();
    var modelNameStringValue = modelName.getValue();
    if (modelNameStringValue.contains("BP_")) {
      log.warn(
          "DEPRECATED detectableObjectModelName {} is still used. Should only use {} for now",
          modelNameStringValue,
          modelNameStringValue.replace("BP_", ""));
    }
    switch (modelNameStringValue) {
      case "TOITURE", "BP_TOITURE" -> objectTypes.addAll(detectableObjectTypeForToitureModel());
      case "LOM", "BP_LOM" -> objectTypes.addAll(detectableObjectTypeForLomModel());
      case "ZAN", "BP_ZAN" -> objectTypes.addAll(detectableObjectTypeForZanModel());
      case "CLIMAT_RESILIENCE", "BP_CLIMAT_RESILIENCE" ->
          objectTypes.addAll(detectableObjectTypeForClimatResilienceModel());
      case "CONFIRMITE_PLU", "BP_CONFIRMITE_PLU" ->
          objectTypes.addAll(detectableObjectTypeForConformitePluModel());
      case "TROTTOIRS", "BP_TROTTOIRS" ->
          objectTypes.addAll(detectableObjectTypeForTrottoirsModel());
      case "OLD", "BP_OLD" -> objectTypes.addAll(detectableObjectTypeForOldModel());
    }
    return objectTypes;
  }

  private List<DetectableObjectType> detectableObjectTypeForToitureModel() {
    List<DetectableObjectType> objectTypes = new ArrayList<>();
    objectTypes.add(ARBRE);
    objectTypes.add(TOITURE_REVETEMENT);
    objectTypes.add(PANNEAU_PHOTOVOLTAIQUE);
    objectTypes.add(MOISISSURE);
    objectTypes.add(USURE);
    objectTypes.add(FISSURE_CASSURE);
    objectTypes.add(OBSTACLE);
    objectTypes.add(CHEMINEE);
    objectTypes.add(HUMIDITE);
    objectTypes.add(RISQUE_FEU);
    return objectTypes;
  }

  private List<DetectableObjectType> detectableObjectTypeForLomModel() {
    List<DetectableObjectType> objectTypes = new ArrayList<>();
    objectTypes.add(PASSAGE_PIETON);
    objectTypes.add(TROTTOIR);
    objectTypes.add(VOIE_CARROSSABLE);
    return objectTypes;
  }

  private List<DetectableObjectType> detectableObjectTypeForClimatResilienceModel() {
    List<DetectableObjectType> objectTypes = new ArrayList<>();
    objectTypes.add(PARKING);
    objectTypes.add(PANNEAU_PHOTOVOLTAIQUE);
    objectTypes.add(ARBRE);
    objectTypes.add(ESPACE_VERT);
    return objectTypes;
  }

  private List<DetectableObjectType> detectableObjectTypeForZanModel() {
    List<DetectableObjectType> objectTypes = new ArrayList<>();
    objectTypes.add(ARBRE);
    objectTypes.add(ESPACE_VERT);
    objectTypes.add(TOITURE_REVETEMENT);
    objectTypes.add(VOIE_CARROSSABLE);
    objectTypes.add(TROTTOIR);
    objectTypes.add(PARKING);
    return objectTypes;
  }

  private List<DetectableObjectType> detectableObjectTypeForConformitePluModel() {
    List<DetectableObjectType> objectTypes = new ArrayList<>();
    objectTypes.add(TOITURE_REVETEMENT);
    objectTypes.add(ARBRE);
    objectTypes.add(VELUX);
    objectTypes.add(PANNEAU_PHOTOVOLTAIQUE);
    objectTypes.add(ESPACE_VERT);
    objectTypes.add(PISCINE);
    return objectTypes;
  }

  private List<DetectableObjectType> detectableObjectTypeForTrottoirsModel() {
    List<DetectableObjectType> objectTypes = new ArrayList<>();
    objectTypes.add(TROTTOIR);
    objectTypes.add(VOIE_CARROSSABLE);
    objectTypes.add(ARBRE);
    objectTypes.add(ESPACE_VERT_PARKING);
    return objectTypes;
  }

  private List<DetectableObjectType> detectableObjectTypeForOldModel() {
    List<DetectableObjectType> objectTypes = new ArrayList<>();
    objectTypes.add(ARBRE);
    objectTypes.add(ESPACE_VERT);
    objectTypes.add(TOITURE_REVETEMENT);
    objectTypes.add(VOIE_CARROSSABLE);
    objectTypes.add(TROTTOIR);
    objectTypes.add(PARKING);
    objectTypes.add(RISQUE_FEU);
    return objectTypes;
  }

  public List<DetectableObjectConfiguration> mapDefaultConfigurationsFromModel(
      String detectionId, ModelName modelName) {
    var objectTypes = mapFromModel(modelName);
    return objectTypes.stream()
        .map(
            detectableObjectType -> {
              var objectType = toDomain(detectableObjectType);
              return DetectableObjectConfiguration.builder()
                  .id(randomUUID().toString())
                  .detectionId(detectionId)
                  .objectType(objectType)
                  .detectionJobId(null)
                  .minConfidenceForDetection(minimumConfidenceForDetection(detectableObjectType))
                  .bucketStorageName(null) // default bucket storage
                  .build();
            })
        .collect(Collectors.toList());
  }

  private Double minimumConfidenceForDetection(DetectableObjectType objectType) {
    switch (objectType) {
      case TROTTOIR -> {
        return 0.252;
      }
      case PISCINE, PANNEAU_PHOTOVOLTAIQUE -> {
        return 0.27;
      }
      case TOITURE_REVETEMENT, LINE, BATI_TUILES, PARKING, BATI_BETON, BATI_AUTRES -> {
        return 0.252;
      }
      case ARBRE -> {
        return 0.2504;
      }
      case PASSAGE_PIETON -> {
        return 0.29;
      }
      case ESPACE_VERT -> {
        return 0.251;
      }
      case BATI_ARDOISE -> {
        return 0.255;
      }
      case VOIE_CARROSSABLE -> {
        return 0.0;
      }
      case MOISISSURE -> {
        return 0.0;
      }
      case USURE -> {
        return 0.0;
      }
      case FISSURE_CASSURE -> {
        return 0.0;
      }
      case OBSTACLE -> {
        return 0.0;
      }
      case CHEMINEE -> {
        return 0.0;
      }
      case HUMIDITE -> {
        return 0.0;
      }
      case RISQUE_FEU -> {
        return 0.0;
      }
      case VELUX -> {
        return 0.0;
      }
      case ESPACE_VERT_PARKING -> {
        return 0.0;
      }
      default -> {
        return DEFAULT_CONFIDENCE;
      }
    }
  }
}
