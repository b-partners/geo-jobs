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
    if (o instanceof BPToitureModel) {
      BPToitureModel model = (BPToitureModel) o;
      if (model.getArbre().booleanValue()) {
        objectTypes.add(ARBRE);
      }
      if (model.getToitureRevetement().booleanValue()) {
        objectTypes.add(TOITURE_REVETEMENT);
      }
      if (model.getPanneauPhotovoltaique().booleanValue()) {
        objectTypes.add(PANNEAU_PHOTOVOLTAIQUE);
      }
      // TODO: add other detectableTypes in BPToitureModel not handled yet
    } else if (o instanceof BPLomModel) {
      BPLomModel model = (BPLomModel) o;
      if (model.getPassagePieton().booleanValue()) {
        objectTypes.add(PASSAGE_PIETON);
      }
      if (model.getTrottoir().booleanValue()) {
        objectTypes.add(TROTTOIR);
      }
      // TODO: add other detectableTypes in BPLomModel not handled yet
    } else if (o instanceof BPZanModel) {
      BPZanModel model = (BPZanModel) o;
      if (model.getArbre().booleanValue()) {
        objectTypes.add(ARBRE);
      }
      if (model.getEspaceVert().booleanValue()) {
        objectTypes.add(ESPACE_VERT);
      }

      if (model.getToiture().booleanValue()) {
        objectTypes.add(TOITURE_REVETEMENT);
      }
      if (model.getVoieCarrossable().booleanValue()) {
        objectTypes.add(VOIE_CARROSSABLE);
      }
      if (model.getTrottoir().booleanValue()) {
        objectTypes.add(TROTTOIR);
      }
      if (model.getParking().booleanValue()) {
        objectTypes.add(PARKING);
      }
    } else {
      throw new ApiException(SERVER_EXCEPTION, "Unknown instance of object " + o.getClass());
    }
    return objectTypes;
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
