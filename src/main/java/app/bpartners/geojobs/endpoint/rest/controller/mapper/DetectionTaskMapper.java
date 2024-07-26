package app.bpartners.geojobs.endpoint.rest.controller.mapper;

import static java.time.Instant.now;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.repository.DetectedTileRepository;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.detection.MachineDetectedObject;
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
    var detectedObjects = machineDetectedTile.getMachineDetectedObjects();
    return new DetectedTile()
        .tileId(tile.getId())
        .creationDatetime(tile.getCreationDatetime())
        .detectedObjects(detectedObjects.stream().map(this::toRest).toList())
        .status(null) // TODO: status of detection task already given before or tiling status ?
        .bucketPath(tile.getBucketPath());
  }

  private DetectedObject toRest(MachineDetectedObject machineDetectedObject) {
    return new DetectedObject()
        .detectedObjectType(toRest(machineDetectedObject.getDetectableObjectType()))
        .feature(machineDetectedObject.getFeature())
        .confidence(BigDecimal.valueOf(machineDetectedObject.getComputedConfidence()))
        .detectorVersion("TODO"); // TODO
  }

  private DetectableObjectType toRest(
      app.bpartners.geojobs.repository.model.detection.DetectableType detectableType) {
    if (detectableType == null) return null;
    switch (detectableType) {
      case SOLAR_PANEL -> {
        return DetectableObjectType.SOLAR_PANEL;
      }
      case ROOF -> {
        return DetectableObjectType.ROOF;
      }
      case TREE -> {
        return DetectableObjectType.TREE;
      }
      case POOL -> {
        return DetectableObjectType.POOL;
      }
      case PATHWAY -> {
        return DetectableObjectType.PATHWAY;
      }
      default -> throw new NotImplementedException(
          "Unknown Detectable Object Type " + detectableType);
    }
  }
}
