package app.bpartners.geojobs.service.detection;

import static app.bpartners.geojobs.endpoint.rest.model.MultiPolygon.TypeEnum.POLYGON;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.*;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.*;
import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.geojobs.repository.model.GeoJobType.DETECTION;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.GREEN_SPACE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.LINE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.PATHWAY;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.POOL;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.SIDEWALK;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.SOLAR_PANEL;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.TOITURE_REVETEMENT;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.TREE;
import static app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.DetectionType.HUMAN;
import static app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.DetectionType.MACHINE;
import static app.bpartners.geojobs.service.detection.DetectionResponse.REGION_CONFIDENCE_PROPERTY;
import static app.bpartners.geojobs.service.detection.DetectionResponse.REGION_LABEL_PROPERTY;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.gen.annotator.endpoint.rest.model.Annotation;
import app.bpartners.gen.annotator.endpoint.rest.model.Label;
import app.bpartners.gen.annotator.endpoint.rest.model.Polygon;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.repository.model.detection.*;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.tiling.TileValidator;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class DetectionMapper {
  private final TileValidator tileValidator;

  public MachineDetectedTile toDetectedTile(
      DetectionResponse detectionResponse,
      Tile tile,
      String parcelId,
      String zdjJobId,
      String parcelJobId) {
    String detectedTileId = randomUUID().toString();
    var tileCoordinates = tile.getCoordinates();
    tileValidator.accept(tile);

    var fileData = detectionResponse.getRstRaw().values().stream().toList().getFirst();

    List<DetectionResponse.ImageData.Region> regions =
        fileData.getRegions().values().stream().toList();
    List<DetectedObject> machineDetectedObjects =
        regions.stream()
            .map(region -> toDetectedObject(region, detectedTileId, tileCoordinates.getZ()))
            .toList();

    return MachineDetectedTile.builder()
        .id(detectedTileId)
        .zdjJobId(zdjJobId)
        .parcelJobId(parcelJobId)
        .parcelId(parcelId)
        .tile(tile)
        .bucketPath(tile.getBucketPath())
        .detectedObjects(machineDetectedObjects)
        .creationDatetime(now())
        .build();
  }

  public DetectedObject toDetectedObject(
      DetectionResponse.ImageData.Region region, String detectedTileId, Integer zoom) {
    var regionAttributes = region.getRegionAttributes();
    var label = regionAttributes.get(REGION_LABEL_PROPERTY);
    Double confidence;
    try {
      confidence = Double.valueOf(regionAttributes.get(REGION_CONFIDENCE_PROPERTY));
    } catch (NumberFormatException e) {
      confidence = null;
    }
    var polygon = region.getShapeAttributes();
    var objectId = randomUUID().toString();
    return DetectedObject.builder()
        .id(objectId)
        .detectedTileId(detectedTileId)
        .type(MACHINE)
        .detectedObjectType(
            DetectableObjectType.builder()
                .id(randomUUID().toString())
                .objectId(objectId)
                .detectableType(toDetectableType(label))
                .build())
        .feature(toFeature(polygon, zoom))
        .computedConfidence(confidence)
        .build();
  }

  private static DetectableType toDetectableType(String label) {
    return switch (label.toUpperCase()) {
      case "ROOF" -> DetectableType.TOITURE_REVETEMENT;
      case "SOLAR_PANEL" -> DetectableType.SOLAR_PANEL;
      case "TREE" -> DetectableType.TREE;
      case "PATHWAY" -> DetectableType.PATHWAY;
      case "POOL" -> DetectableType.POOL;
      default -> throw new IllegalStateException("Unexpected value: " + label.toLowerCase());
    };
  }

  private static Feature toFeature(
      DetectionResponse.ImageData.ShapeAttributes shapeAttributes, int zoom) {
    List<List<BigDecimal>> coordinates = new ArrayList<>();
    var allX = shapeAttributes.getAllPointsX();
    var allY = shapeAttributes.getAllPointsY();
    IntStream.range(0, allX.size())
        .forEach(i -> coordinates.add(List.of(allX.get(i), allY.get(i))));
    return new Feature()
        .id(randomUUID().toString())
        .zoom(zoom)
        .geometry(new MultiPolygon().type(POLYGON).coordinates(List.of(List.of(coordinates))));
  }

  /*
  TODO: custom for saving detection task
  public DetectionTask toDomain(Tile tile, String zoneDetectionJobId) {
    String taskId = randomUUID().toString();
    return DetectionTask.builder()
        .id(taskId)
        .jobId(zoneDetectionJobId)
        .tile(tile)
        .statusHistory(
            List.of(
                TaskStatus.builder()
                    .health(UNKNOWN)
                    .progression(PENDING)
                    .jobType(DETECTION)
                    .creationDatetime(now())
                    .taskId(taskId)
                    .build()))
        .submissionInstant(now())
        .build();
  }*/

  public ZoneDetectionJob fromTilingJob(ZoneTilingJob tilingJob) {
    String zoneDetectionJobId = randomUUID().toString();
    var detectionJob =
        ZoneDetectionJob.builder()
            .id(zoneDetectionJobId)
            .zoneTilingJob(tilingJob)
            .detectionType(MACHINE)
            .zoneName(tilingJob.getZoneName())
            .emailReceiver(tilingJob.getEmailReceiver())
            .submissionInstant(now())
            .build();
    detectionJob.hasNewStatus(
        JobStatus.builder()
            .jobId(zoneDetectionJobId)
            .id(randomUUID().toString())
            .creationDatetime(now())
            .jobType(DETECTION)
            .progression(PENDING)
            .health(UNKNOWN)
            .build());
    return detectionJob;
  }

  public Status.ProgressionStatus getProgressionStatus(
      app.bpartners.gen.annotator.endpoint.rest.model.JobStatus annotationJobStatus) {
    if (annotationJobStatus == null) return null;
    return switch (annotationJobStatus.getValue()) {
      case "COMPLETED", "FAILED" -> FINISHED;
      case "STARTED" -> PROCESSING;
      case "PENDING", "READY", "TO_REVIEW", "TO_CORRECT" -> PENDING;
      default ->
          throw new ApiException(
              SERVER_EXCEPTION, "Unknown annotationJobStatus " + annotationJobStatus.getValue());
    };
  }

  public Status.HealthStatus getHealthStatus(
      app.bpartners.gen.annotator.endpoint.rest.model.JobStatus annotationJobStatus) {
    if (annotationJobStatus == null) return null;
    return switch (annotationJobStatus.getValue()) {
      case "COMPLETED" -> SUCCEEDED;
      case "FAILED" -> FAILED;
      default -> UNKNOWN;
    };
  }

  public List<DetectedObject> toHumanDetectedObject(
      int zoom, String tileId, List<Annotation> annotations) {
    return annotations.stream()
        .map(
            ann -> {
              var objectId = randomUUID().toString();
              if (ann.getPolygon() == null) return null;
              var confidence = ann.getComment() != null ? getConfidence(ann.getComment()) : null;
              return DetectedObject.builder()
                  .id(objectId)
                  .detectedObjectType(toDetectableObjectType(objectId, ann.getLabel()))
                  .computedConfidence(confidence)
                  .feature(toFeature(zoom, ann.getPolygon()))
                  .detectedTileId(tileId)
                  .type(HUMAN)
                  .build();
            })
        .toList();
  }

  private DetectableObjectType toDetectableObjectType(String objectId, Label label) {
    if (label.getName() == null) {
      throw new IllegalArgumentException("label.name cannot be null");
    }
    return switch (label.getName().toUpperCase()) {
      case "ROOF" -> create(objectId, TOITURE_REVETEMENT);
      case "SOLAR_PANEL" -> create(objectId, SOLAR_PANEL);
      case "POOL" -> create(objectId, POOL);
      case "TREE" -> create(objectId, TREE);
      case "SIDEWALK" -> create(objectId, SIDEWALK);
      case "PATHWAY" -> create(objectId, PATHWAY);
      case "LINE" -> create(objectId, LINE);
      case "GREEN_SPACE" -> create(objectId, GREEN_SPACE);
      default ->
          throw new IllegalStateException("Unexpected value: " + label.getName().toUpperCase());
    };
  }

  private double getConfidence(String comment) {
    var splitInput = Arrays.stream(comment.split("=")).toList();
    return new BigDecimal(splitInput.getLast()).doubleValue() / 100;
  }

  private DetectableObjectType create(String objectId, DetectableType detectableType) {
    return DetectableObjectType.builder()
        .id(randomUUID().toString())
        .objectId(objectId)
        .detectableType(detectableType)
        .build();
  }

  private Feature toFeature(int zoom, Polygon polygon) {
    if (polygon.getPoints() == null) return null;
    var coordinates =
        polygon.getPoints().stream()
            .map(
                point -> {
                  if (point.getX() == null || point.getY() == null) return null;
                  return List.of(
                      List.of(
                          List.of(
                              BigDecimal.valueOf(point.getX()), BigDecimal.valueOf(point.getY()))));
                })
            .toList();
    return new Feature()
        .id(randomUUID().toString())
        .zoom(zoom)
        .geometry(new MultiPolygon().coordinates(coordinates));
  }
}
