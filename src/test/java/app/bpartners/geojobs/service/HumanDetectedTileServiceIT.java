package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.PATHWAY;
import static app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.DetectionType.HUMAN;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectType;
import app.bpartners.geojobs.repository.model.detection.DetectedObject;
import app.bpartners.geojobs.repository.model.detection.HumanDetectedTile;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.detection.HumanDetectedTileService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class HumanDetectedTileServiceIT extends FacadeIT {
  private static final String HUMAN_DETECTED_TILE_ID = "human_detected_tile_id";
  private static final String MACHINE_DETECTED_TILE_ID = "human_detected_tile_id";
  private static final String JOB_ID = "job_id";
  @Autowired public HumanDetectedTileService subject;

  HumanDetectedTile toCreate() {
    return HumanDetectedTile.builder()
        .id(HUMAN_DETECTED_TILE_ID)
        .tile(
            Tile.builder()
                .id(randomUUID().toString())
                .coordinates(new TileCoordinates().x(5000).y(2000).z(20))
                .build())
        .jobId(JOB_ID)
        .annotationJobId(randomUUID().toString())
        .machineDetectedTileId(MACHINE_DETECTED_TILE_ID)
        .imageSize(1024)
        .detectedObjects(List.of(detectedObject()))
        .build();
  }

  DetectedObject detectedObject() {
    String objectId = randomUUID().toString();
    return DetectedObject.builder()
        .id(objectId)
        .detectedTileId(HUMAN_DETECTED_TILE_ID)
        .type(HUMAN)
        .detectedObjectType(
            DetectableObjectType.builder()
                .id(randomUUID().toString())
                .objectId(objectId)
                .detectableType(PATHWAY)
                .build())
        .computedConfidence(0.95)
        .feature(new Feature())
        .build();
  }

  @Test
  void create_and_read_humane_detected_tile() {
    var actual = subject.saveAll(List.of(toCreate()));
    var persisted = subject.getByJobId(JOB_ID);

    assertEquals(persisted.getFirst().getId(), actual.getFirst().getId());
  }
}
