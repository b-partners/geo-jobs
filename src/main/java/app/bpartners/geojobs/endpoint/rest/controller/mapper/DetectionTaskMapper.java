package app.bpartners.geojobs.endpoint.rest.controller.mapper;

import static java.time.Instant.now;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.repository.DetectedTileRepository;
import app.bpartners.geojobs.repository.model.Parcel;
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
    List<app.bpartners.geojobs.repository.model.detection.DetectedTile> detectedTiles =
        parcel == null ? List.of() : detectedTileRepository.findAllByParcelId(parcel.getId());
    var lastDetectedTileCreationDatetime =
        detectedTiles.stream()
            .max(
                Comparator.comparing(
                    app.bpartners.geojobs.repository.model.detection.DetectedTile
                        ::getCreationDatetime))
            .orElse(
                app.bpartners.geojobs.repository.model.detection.DetectedTile.builder()
                    .creationDatetime(now())
                    .build())
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
        .detectedTiles(
            detectedTiles.stream()
                .map(
                    tile ->
                        toRest(
                            (app.bpartners.geojobs.repository.model.detection.DetectedTile) tile))
                .toList());
  }

  private DetectedTile toRest(
      app.bpartners.geojobs.repository.model.detection.DetectedTile detectedTile) {
    var tile = detectedTile.getTile();
    var detectedObjects = detectedTile.getDetectedObjects();
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
      default ->
          throw new NotImplementedException("Unknown Detectable Object Type " + detectableType);
    }
  }
}
