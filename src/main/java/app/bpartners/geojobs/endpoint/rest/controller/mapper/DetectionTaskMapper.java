package app.bpartners.geojobs.endpoint.rest.controller.mapper;

import static app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType.ARBRE;
import static app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType.BATI_ARDOISE;
import static app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType.BATI_AUTRES;
import static app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType.BATI_BETON;
import static app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType.BATI_TUILES;
import static app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType.PANNEAU_PHOTOVOLTAIQUE;
import static app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType.PASSAGE_PIETON;
import static app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType.PISCINE;
import static app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType.TOITURE_REVETEMENT;
import static app.bpartners.geojobs.endpoint.rest.model.Status.ProgressionEnum.PENDING;
import static java.time.Instant.now;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType;
import app.bpartners.geojobs.endpoint.rest.model.DetectedObject;
import app.bpartners.geojobs.endpoint.rest.model.DetectedParcel;
import app.bpartners.geojobs.endpoint.rest.model.DetectedTile;
import app.bpartners.geojobs.endpoint.rest.model.Status;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.repository.MachineDetectedTileRepository;
import app.bpartners.geojobs.repository.model.ParcelTask;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import app.bpartners.geojobs.repository.model.detection.MachineDetectedTile;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class DetectionTaskMapper {
  private final MachineDetectedTileRepository machineDetectedTileRepository;

  public DetectedParcel toRest(String jobId, ParcelTask parcelTask) {
    var parcel = parcelTask.getParcel();
    if (parcel == null) {
      return null;
    }
    List<MachineDetectedTile> machineDetectedTiles =
        machineDetectedTileRepository.findAllByParcelId(parcel.getId());
    var lastDetectedTileCreationDatetime =
        machineDetectedTiles.stream()
            .max(Comparator.comparing(MachineDetectedTile::getCreationDatetime))
            .orElse(MachineDetectedTile.builder().creationDatetime(now()).build())
            .getCreationDatetime();
    var parcelTaskStatus = parcelTask.getStatus();
    return new DetectedParcel()
        .id(randomUUID().toString())
        .creationDatetime(lastDetectedTileCreationDatetime)
        .detectionJobIb(jobId)
        .parcelId(parcel.getId())
        .status(
            ofNullable(parcelTaskStatus)
                .map(
                    status ->
                        new Status()
                            .health(StatusMapper.toHealthStatus(parcelTaskStatus.getHealth()))
                            .progression(
                                StatusMapper.toProgressionEnum(parcelTaskStatus.getProgression()))
                            .creationDatetime(parcelTaskStatus.getCreationDatetime()))
                .orElse(
                    new Status()
                        .progression(PENDING)
                        .health(Status.HealthEnum.UNKNOWN)
                        .creationDatetime(lastDetectedTileCreationDatetime)))
        .detectedTiles(machineDetectedTiles.stream().map(this::toRest).toList());
  }

  private DetectedTile toRest(MachineDetectedTile machineDetectedTile) {
    var tile = machineDetectedTile.getTile();
    var detectedObjects = machineDetectedTile.getDetectedObjects();
    return new DetectedTile()
        .tileId(tile.getId())
        .creationDatetime(tile.getCreationDatetime())
        .detectedObjects(detectedObjects.stream().map(this::toRest).toList())
        .status(null) // TODO: status of detection task already given before or tiling status ?
        .bucketPath(tile.getBucketPath());
  }

  private DetectedObject toRest(
      app.bpartners.geojobs.repository.model.detection.DetectedObject detectedObject) {
    return new DetectedObject()
        .detectedObjectType(toRest(detectedObject.getDetectableObjectType()))
        .feature(detectedObject.getFeature())
        .confidence(BigDecimal.valueOf(detectedObject.getComputedConfidence()))
        .detectorVersion("TODO"); // TODO
  }

  private DetectableObjectType toRest(DetectableType detectableType) {
    if (detectableType == null) return null;
    return switch (detectableType) {
      case PANNEAU_PHOTOVOLTAIQUE -> PANNEAU_PHOTOVOLTAIQUE;
      case TOITURE_REVETEMENT -> TOITURE_REVETEMENT;
      case ARBRE -> ARBRE;
      case PISCINE -> PISCINE;
      case PASSAGE_PIETON -> PASSAGE_PIETON;
      case BATI_TUILES -> BATI_TUILES;
      case BATI_BETON -> BATI_BETON;
      case BATI_ARDOISE -> BATI_ARDOISE;
      case BATI_AUTRES -> BATI_AUTRES;
      case TROTTOIR,
              LINE,
              ESPACE_VERT,
              VOIE_CARROSSABLE,
              PARKING,
              MOISISSURE,
              USURE,
              FISSURE_CASSURE,
              OBSTACLE,
              CHEMINEE,
              HUMIDITE,
              RISQUE_FEU,
              VELUX ->
          throw new NotImplementedException("Unsupported detection: " + detectableType);
    };
  }
}
