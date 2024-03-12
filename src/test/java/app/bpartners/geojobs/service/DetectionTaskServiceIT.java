package app.bpartners.geojobs.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.repository.DetectableObjectConfigurationRepository;
import app.bpartners.geojobs.repository.DetectedTileRepository;
import app.bpartners.geojobs.repository.DetectionTaskRepository;
import app.bpartners.geojobs.repository.ZoneDetectionJobRepository;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.detection.*;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.detection.DetectionTaskService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class DetectionTaskServiceIT extends FacadeIT {
  public static final String JOB_ID = "jobId";
  @Autowired private DetectionTaskService subject;
  @Autowired private ZoneDetectionJobRepository jobRepository;
  @Autowired private DetectedTileRepository detectedTileRepository;
  @Autowired private DetectableObjectConfigurationRepository objectConfigurationRepository;
  @Autowired private DetectionTaskRepository detectionTaskRepository;
  private static final double CONFIDENCE = 0.67;

  private static DetectedTile detectedTile(double confidence) {
    return DetectedTile.builder()
        .id("detectedTileId")
        .jobId(JOB_ID)
        .parcelId("parcelId")
        .detectedObjects(
            List.of(
                DetectedObject.builder()
                    .id("detectedObjectId")
                    .computedConfidence(confidence)
                    .detectedTileId("detectedTileId")
                    .feature(new Feature().id("featureId"))
                    .detectedObjectTypes(
                        List.of(
                            DetectableObjectType.builder()
                                .id("detectableObjectTypeId")
                                .detectableType(DetectableType.ROOF)
                                .objectId("detectedObjectId")
                                .build()))
                    .build()))
        .bucketPath("dummyPath")
        .creationDatetime(null)
        .tile(new Tile())
        .build();
  }

  @BeforeEach
  void setUp() {
    jobRepository.save(ZoneDetectionJob.builder().id(JOB_ID).build());
    detectionTaskRepository.save(
        DetectionTask.builder()
            .id("detectionTaskId")
            .parcels(List.of(Parcel.builder().id("parcelId").build()))
            .build());
    detectedTileRepository.save(detectedTile(CONFIDENCE));
    objectConfigurationRepository.save(
        DetectableObjectConfiguration.builder()
            .id("detectableObjectConfigurationId")
            .confidence(1.0)
            .objectType(DetectableType.ROOF)
            .detectionJobId(JOB_ID)
            .build());
  }

  @AfterEach
  void tearDown() {
    objectConfigurationRepository.deleteById("detectableObjectConfigurationId");
    detectedTileRepository.deleteById("detectedTileId");
    detectionTaskRepository.deleteById("detectionTaskId");
    jobRepository.deleteById(JOB_ID);
  }

  @Test
  void read_in_doubt_tiles() {
    List<DetectedTile> expected = List.of(detectedTile(CONFIDENCE));

    List<DetectedTile> actual = subject.findInDoubtTilesByJobId(JOB_ID);

    assertEquals(expected, actual.stream().peek(tile -> tile.setCreationDatetime(null)).toList());
  }
}
