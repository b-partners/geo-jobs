package app.bpartners.geojobs.service.detection;

import static app.bpartners.geojobs.endpoint.rest.model.MultiPolygon.TypeEnum.POLYGON;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING;
import static app.bpartners.geojobs.repository.model.GeoJobType.DETECTION;
import static app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.DetectionType.MACHINE;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectType;
import app.bpartners.geojobs.repository.model.detection.DetectedObject;
import app.bpartners.geojobs.repository.model.detection.DetectedTile;
import app.bpartners.geojobs.repository.model.detection.DetectionTask;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.tiling.TileValidator;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class DetectionMapper {
  private static final TileValidator tileValidator = new TileValidator();

  public static DetectedTile toDetectedTile(
      DetectionResponse detectionResponse, Tile tile, String jobId) {
    String detectedTileId = randomUUID().toString();
    var tileCoordinates = tile.getCoordinates();
    tileValidator.accept(tile);

    var fileData = detectionResponse.getRstRaw().values().stream().toList().get(0);

    List<DetectionResponse.ImageData.Region> regions =
        fileData.getRegions().values().stream().toList();
    List<DetectedObject> detectedObjects =
        regions.stream()
            .map(region -> toDetectedObject(region, detectedTileId, tileCoordinates.getZ()))
            .toList();

    return DetectedTile.builder()
        .id(detectedTileId)
        .jobId(jobId)
        .tile(tile)
        .bucketPath(tile.getBucketPath())
        .detectedObjects(detectedObjects)
        .creationDatetime(now())
        .build();
  }

  public static DetectedObject toDetectedObject(
      DetectionResponse.ImageData.Region region, String detectedTileId, Integer zoom) {
    var regionAttributes = region.getRegionAttributes();
    var label = regionAttributes.get("label");
    Double confidence;
    try {
      confidence = Double.valueOf(regionAttributes.get("confidence"));
    } catch (NumberFormatException e) {
      confidence = null;
    }
    var polygon = region.getShapeAttributes();
    var objectId = randomUUID().toString();
    return DetectedObject.builder()
        .id(objectId)
        .detectedTileId(detectedTileId)
        .detectedObjectTypes(
            List.of(
                DetectableObjectType.builder()
                    .id(randomUUID().toString())
                    .objectId(objectId)
                    .detectableType(toDetectableType(label))
                    .build()))
        .feature(toFeature(polygon, zoom))
        .computedConfidence(confidence)
        .build();
  }

  private static DetectableObjectType.DetectableType toDetectableType(String label) {
    return switch (label.toUpperCase()) {
      case "ROOF" -> DetectableObjectType.DetectableType.ROOF;
      case "SOLAR_PANEL" -> DetectableObjectType.DetectableType.SOLAR_PANEL;
      case "TREE" -> DetectableObjectType.DetectableType.TREE;
      case "PATHWAY" -> DetectableObjectType.DetectableType.PATHWAY;
      case "POOL" -> DetectableObjectType.DetectableType.POOL;
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
  }

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
}