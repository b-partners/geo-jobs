package app.bpartners.geojobs.endpoint.rest.controller.mapper;

import static java.time.Instant.now;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType;
import app.bpartners.geojobs.endpoint.rest.model.DetectedObject;
import app.bpartners.geojobs.endpoint.rest.model.DetectedParcel;
import app.bpartners.geojobs.endpoint.rest.model.DetectedTile;
import app.bpartners.geojobs.endpoint.rest.model.Status;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.repository.DetectedTileRepository;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.detection.MachineDetectedTile;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class DetectionTaskMapper {
  private final DetectedTileRepository detectedTileRepository;

  public DetectedParcel toRest(String jobId, Parcel parcel) {
    List<MachineDetectedTile> machineDetectedTiles =
        parcel == null ? List.of() : detectedTileRepository.findAllByParcelId(parcel.getId());
    var lastDetectedTileCreationDatetime =
        machineDetectedTiles.stream()
            .max(Comparator.comparing(MachineDetectedTile::getCreationDatetime))
            .orElse(MachineDetectedTile.builder().creationDatetime(now()).build())
            .getCreationDatetime();
    return new DetectedParcel()
        .id(randomUUID().toString()) // TODO DetectedParcel must be persisted
        .creationDatetime(
            lastDetectedTileCreationDatetime) // TODO change when DetectedParcel is persisted
        .detectionJobIb(jobId)
        .parcelId(parcel == null ? null : parcel.getId())
        .status(
            ofNullable(parcel == null ? null : parcel.getParcelContent().getDetectionStatus())
                .map(
                    status ->
                        new Status()
                            .health(StatusMapper.toHealthStatus(status.getHealth()))
                            .progression(StatusMapper.toProgressionEnum(status.getProgression()))
                            .creationDatetime(status.getCreationDatetime()))
                .orElse(null))
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

  private DetectableObjectType toRest(
      app.bpartners.geojobs.repository.model.detection.DetectableType detectableType) {
    if (detectableType == null) return null;
    switch (detectableType) {
      case PANNEAU_PHOTOVOLTAIQUE -> {
        return DetectableObjectType.PANNEAU_PHOTOVOLTAIQUE;
      }
      case TOITURE_REVETEMENT -> {
        return DetectableObjectType.TOITURE_REVETEMENT;
      }
      case TREE -> {
        return DetectableObjectType.TREE;
      }
      case PISCINE -> {
        return DetectableObjectType.PISCINE;
      }
      case PASSAGE_PIETON -> {
        return DetectableObjectType.PASSAGE_PIETON;
      }
      default ->
          throw new NotImplementedException("Unknown Detectable Object Type " + detectableType);
    }
  }
}
