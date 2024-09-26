package app.bpartners.geojobs.endpoint.rest.controller.mapper;

import static app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType.*;
import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.rest.model.*;
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
      default -> throw new NotImplementedException("Unknown detectable object type " + rest);
    };
  }

  public DetectableObjectType toRest(DetectableType domain) {
    return switch (domain) {
      case PISCINE -> DetectableObjectType.PISCINE;
      case TOITURE_REVETEMENT -> DetectableObjectType.TOITURE_REVETEMENT;
      case ARBRE -> DetectableObjectType.ARBRE;
      case PASSAGE_PIETON -> DetectableObjectType.PASSAGE_PIETON;
      case PANNEAU_PHOTOVOLTAIQUE -> DetectableObjectType.PANNEAU_PHOTOVOLTAIQUE;
      case TROTTOIR -> DetectableObjectType.TROTTOIR;
      case LINE -> DetectableObjectType.LINE;
      case ESPACE_VERT -> DetectableObjectType.ESPACE_VERT;
      case VOIE_CARROSSABLE -> DetectableObjectType.VOIE_CARROSSABLE;
      case MOISISSURE -> DetectableObjectType.MOISISSURE;
      case USURE -> DetectableObjectType.USURE;
      case FISSURE_CASSURE -> DetectableObjectType.FISSURE_CASSURE;
      case OBSTACLE -> DetectableObjectType.OBSTACLE;
      case CHEMINEE -> DetectableObjectType.CHEMINEE;
      case HUMIDITE -> DetectableObjectType.HUMIDITE;
      case RISQUE_FEU -> DetectableObjectType.RISQUE_FEU;
      case VELUX -> DetectableObjectType.VELUX;
      default -> throw new NotImplementedException("Unknown detectable object type " + domain);
    };
  }

  public List<DetectableObjectType> mapFromModel(Object o) {
    List<DetectableObjectType> objectTypes = new ArrayList<>();

    switch (o) {
      case BPToitureModel model ->
          objectTypes.addAll(detectableObjectTypeFromBPToitureModel(model));
      case BPLomModel model -> objectTypes.addAll(detectableObjectTypeFromBPLomModel(model));
      case BPZanModel model -> objectTypes.addAll(detectableObjectTypeFromBPZanModel(model));
      case BPClimatResilienceModel model ->
          objectTypes.addAll(detectableObjectTypeFromBPClimatResilienceModel(model));
      case BPConformitePluModel model ->
          objectTypes.addAll(detectableObjectTypeFromBPConformitePluModel(model));
      case BPTrottoirsModel model ->
          objectTypes.addAll(detectableObjectTypeFromBPTrottoirsModel(model));
      case BPOldModel model -> objectTypes.addAll(detectableObjectTypeFromBPOldModel(model));
      default ->
          throw new ApiException(SERVER_EXCEPTION, "Unknown instance of object " + o.getClass());
    }

    return objectTypes;
  }

  private List<DetectableObjectType> detectableObjectTypeFromBPToitureModel(BPToitureModel model) {
    List<DetectableObjectType> objectTypes = new ArrayList<>();
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
    return objectTypes;
  }

  private List<DetectableObjectType> detectableObjectTypeFromBPLomModel(BPLomModel model) {
    List<DetectableObjectType> objectTypes = new ArrayList<>();
    addIfTrue(model.getPassagePieton(), objectTypes, PASSAGE_PIETON);
    addIfTrue(model.getTrottoir(), objectTypes, TROTTOIR);
    addIfTrue(model.getVoieCarrosable(), objectTypes, VOIE_CARROSSABLE);
    return objectTypes;
  }

  private List<DetectableObjectType> detectableObjectTypeFromBPClimatResilienceModel(
      BPClimatResilienceModel model) {
    List<DetectableObjectType> objectTypes = new ArrayList<>();
    addIfTrue(model.getParking(), objectTypes, PARKING);
    addIfTrue(model.getPanneauPhotovoltaique(), objectTypes, PANNEAU_PHOTOVOLTAIQUE);
    addIfTrue(model.getArbre(), objectTypes, ARBRE);
    addIfTrue(model.getEspaceVert(), objectTypes, ESPACE_VERT);
    return objectTypes;
  }

  private List<DetectableObjectType> detectableObjectTypeFromBPZanModel(BPZanModel model) {
    List<DetectableObjectType> objectTypes = new ArrayList<>();
    addIfTrue(model.getArbre(), objectTypes, ARBRE);
    addIfTrue(model.getEspaceVert(), objectTypes, ESPACE_VERT);
    addIfTrue(model.getToiture(), objectTypes, TOITURE_REVETEMENT);
    addIfTrue(model.getVoieCarrossable(), objectTypes, VOIE_CARROSSABLE);
    addIfTrue(model.getTrottoir(), objectTypes, TROTTOIR);
    addIfTrue(model.getParking(), objectTypes, PARKING);
    return objectTypes;
  }

  private List<DetectableObjectType> detectableObjectTypeFromBPConformitePluModel(
      BPConformitePluModel model) {
    List<DetectableObjectType> objectTypes = new ArrayList<>();
    addIfTrue(model.getToiture(), objectTypes, TOITURE_REVETEMENT);
    addIfTrue(model.getArbre(), objectTypes, ARBRE);
    addIfTrue(model.getVelux(), objectTypes, VELUX);
    addIfTrue(model.getPanneauPhotovoltaique(), objectTypes, PANNEAU_PHOTOVOLTAIQUE);
    addIfTrue(model.getEspaceVert(), objectTypes, ESPACE_VERT);
    addIfTrue(model.getPiscine(), objectTypes, PISCINE);
    return objectTypes;
  }

  private List<DetectableObjectType> detectableObjectTypeFromBPTrottoirsModel(
      BPTrottoirsModel model) {
    List<DetectableObjectType> objectTypes = new ArrayList<>();
    addIfTrue(model.getTrottoir(), objectTypes, TROTTOIR);
    addIfTrue(model.getVoieCarrossable(), objectTypes, VOIE_CARROSSABLE);
    addIfTrue(model.getArbre(), objectTypes, ARBRE);
    addIfTrue(model.getEspaceVertParking(), objectTypes, ESPACE_VERT_PARKING);
    return objectTypes;
  }

  private List<DetectableObjectType> detectableObjectTypeFromBPOldModel(BPOldModel model) {
    List<DetectableObjectType> objectTypes = new ArrayList<>();
    addIfTrue(model.getArbre(), objectTypes, ARBRE);
    addIfTrue(model.getEspaceVert(), objectTypes, ESPACE_VERT);
    addIfTrue(model.getToiture(), objectTypes, TOITURE_REVETEMENT);
    addIfTrue(model.getVoieCarrossable(), objectTypes, VOIE_CARROSSABLE);
    addIfTrue(model.getTrottoir(), objectTypes, TROTTOIR);
    addIfTrue(model.getParking(), objectTypes, PARKING);
    addIfTrue(model.getRisqueFeu(), objectTypes, RISQUE_FEU);
    return objectTypes;
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
