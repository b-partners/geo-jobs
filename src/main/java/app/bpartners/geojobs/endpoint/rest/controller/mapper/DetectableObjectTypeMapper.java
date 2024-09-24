package app.bpartners.geojobs.endpoint.rest.controller.mapper;

import static app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType.*;
import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.rest.model.BPLomModel;
import app.bpartners.geojobs.endpoint.rest.model.BPToitureModel;
import app.bpartners.geojobs.endpoint.rest.model.BPZanModel;
import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class DetectableObjectTypeMapper {

  public static final double DEFAULT_CONFIDENCE = 1.0;

  public DetectableType toDomain(DetectableObjectType rest) {
    switch (rest) {
      case PISCINE -> {
        return DetectableType.PISCINE;
      }
      case TOITURE_REVETEMENT -> {
        return DetectableType.TOITURE_REVETEMENT;
      }
      case ARBRE -> {
        return DetectableType.ARBRE;
      }
      case PASSAGE_PIETON -> {
        return DetectableType.PASSAGE_PIETON;
      }
      case PANNEAU_PHOTOVOLTAIQUE -> {
        return DetectableType.PANNEAU_PHOTOVOLTAIQUE;
      }
      default -> throw new NotImplementedException("Unknown detectable object type " + rest);
    }
  }

  public DetectableObjectType toRest(DetectableType domain) {
    switch (domain) {
      case PISCINE -> {
        return PISCINE;
      }
      case TOITURE_REVETEMENT -> {
        return TOITURE_REVETEMENT;
      }
      case ARBRE -> {
        return ARBRE;
      }
      case PASSAGE_PIETON -> {
        return PASSAGE_PIETON;
      }
      case PANNEAU_PHOTOVOLTAIQUE -> {
        return PANNEAU_PHOTOVOLTAIQUE;
      }
      case TROTTOIR -> {
        return TROTTOIR;
      }
      case LINE -> {
        return LINE;
      }
      case ESPACE_VERT -> {
        return ESPACE_VERT;
      }
      default -> throw new NotImplementedException("Unknown detectable object type " + domain);
    }
  }

  public List<DetectableObjectType> mapFromModel(Object o) {
    var objectTypes = new ArrayList<DetectableObjectType>();

    if (o instanceof BPToitureModel model) {
      mapFromBPToitureModel(model, objectTypes);
    } else if (o instanceof BPLomModel model) {
      mapFromBPLomModel(model, objectTypes);
    } else if (o instanceof BPZanModel model) {
      mapFromBPZanModel(model, objectTypes);
    } else {
      throw new ApiException(SERVER_EXCEPTION, "Unknown instance of object " + o.getClass());
    }

    return objectTypes;
  }

  private void mapFromBPToitureModel(BPToitureModel model, List<DetectableObjectType> objectTypes) {
    addIfTrue(model.getArbre(), objectTypes, ARBRE);
    addIfTrue(model.getToitureRevetement(), objectTypes, TOITURE_REVETEMENT);
    addIfTrue(model.getPanneauPhotovoltaique(), objectTypes, PANNEAU_PHOTOVOLTAIQUE);
    addIfTrue(model.getMoisissure(), objectTypes, MOISISSURE);
    addIfTrue(model.getUsure(), objectTypes, USURE);
    addIfTrue(model.getFissureCassure(), objectTypes, FISSURE_CASSURE);
    addIfTrue(model.getObstacle(), objectTypes, OBSTACLE);
    addIfTrue(model.getCheminee(), objectTypes, CHEMINEE);
    addIfTrue(model.getHumidite(), objectTypes, HUMIDITE);
    addIfTrue(model.getRisqueFeu(), objectTypes, RISQUE_FEU);
  }

  private void mapFromBPLomModel(BPLomModel model, List<DetectableObjectType> objectTypes) {
    addIfTrue(model.getPassagePieton(), objectTypes, PASSAGE_PIETON);
    addIfTrue(model.getTrottoir(), objectTypes, TROTTOIR);
    addIfTrue(model.getVoieCarrosable(), objectTypes, VOIE_CARROSSABLE);
  }

  private void mapFromBPZanModel(BPZanModel model, List<DetectableObjectType> objectTypes) {
    addIfTrue(model.getArbre(), objectTypes, ARBRE);
    addIfTrue(model.getEspaceVert(), objectTypes, ESPACE_VERT);
    addIfTrue(model.getToiture(), objectTypes, TOITURE_REVETEMENT);
    addIfTrue(model.getVoieCarrossable(), objectTypes, VOIE_CARROSSABLE);
    addIfTrue(model.getTrottoir(), objectTypes, TROTTOIR);
    addIfTrue(model.getParking(), objectTypes, PARKING);
  }

  private void addIfTrue(
      Boolean condition, List<DetectableObjectType> objectTypes, DetectableObjectType objectType) {
    if (Boolean.TRUE.equals(condition)) {
      objectTypes.add(objectType);
    }
  }

  public List<DetectableObjectConfiguration> mapDefaultConfigurationsFromModel(
      String detectionId, Object o) {
    var objectTypes = mapFromModel(o);
    return objectTypes.stream()
        .map(
            objectType ->
                DetectableObjectConfiguration.builder()
                    .id(randomUUID().toString())
                    .detectionId(detectionId)
                    .objectType(toDomain(objectType))
                    .detectionJobId(null)
                    .confidence(DEFAULT_CONFIDENCE)
                    .bucketStorageName(null) // default bucket storage
                    .build())
        .collect(Collectors.toList());
  }
}
