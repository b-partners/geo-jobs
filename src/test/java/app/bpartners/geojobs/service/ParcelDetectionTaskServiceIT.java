package app.bpartners.geojobs.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.repository.*;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.detection.*;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.detection.DetectionTaskService;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(isolation = Isolation.SERIALIZABLE)
public class ParcelDetectionTaskServiceIT extends FacadeIT {
  public static final String JOB_ID = "jobId";
  public static final double MIN_CONFIDENCE = 0.70;
  @Autowired private DetectionTaskService subject;
  @Autowired private ZoneDetectionJobRepository jobRepository;
  @Autowired private DetectedTileRepository detectedTileRepository;
  @Autowired private DetectableObjectConfigurationRepository objectConfigurationRepository;
  @Autowired private ParcelDetectionTaskRepository parcelDetectionTaskRepository;
  @Autowired private ParcelRepository parcelRepository;
  private static final double UNDER_MIN_CONFIDENCE = 0.67;

  public static DetectedTile detectedTile(
      String jobId, String tileId, String parcelId, String detectedObjectId, double confidence) {
    return DetectedTile.builder()
        .id(tileId)
        .jobId(jobId)
        .parcelId(parcelId)
        .detectedObjects(
            List.of(
                DetectedObject.builder()
                    .id(detectedObjectId)
                    .computedConfidence(confidence)
                    .detectedTileId(tileId)
                    .feature(new Feature().id("featureId"))
                    .detectedObjectTypes(
                        List.of(
                            DetectableObjectType.builder()
                                .id(detectedObjectId)
                                .detectableType(DetectableType.ROOF)
                                .objectId(detectedObjectId)
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
    List<Parcel> parcels = getParcels();
    parcelRepository.saveAll(parcels);
    parcelDetectionTaskRepository.save(
        ParcelDetectionTask.builder().id("detectionTaskId").parcels(parcels).build());
    detectedTileRepository.saveAll(
        List.of(
            detectedTile(JOB_ID, "tile1Id", "parcel1Id", "detectedObjectId1", UNDER_MIN_CONFIDENCE),
            detectedTile(JOB_ID, "tile2Id", "parcel2Id", "detectedObjectId2", 0.70),
            detectedTile(JOB_ID, "tile3Id", "parcel3Id", "detectedObjectId3", 0.71)));
    objectConfigurationRepository.save(
        DetectableObjectConfiguration.builder()
            .id("detectableObjectConfigurationId")
            .confidence(MIN_CONFIDENCE)
            .objectType(DetectableType.ROOF)
            .detectionJobId(JOB_ID)
            .build());
  }

  @NotNull
  private static List<Parcel> getParcels() {
    return List.of(
        Parcel.builder().id("parcel1Id").build(),
        Parcel.builder().id("parcel2Id").build(),
        Parcel.builder().id("parcel3Id").build());
  }

  @AfterEach
  void tearDown() {
    objectConfigurationRepository.deleteById("detectableObjectConfigurationId");
    detectedTileRepository.deleteById("detectedTileId");
    parcelDetectionTaskRepository.deleteById("detectionTaskId");
    jobRepository.deleteById(JOB_ID);
    parcelRepository.deleteAllById(getParcels().stream().map(Parcel::getId).toList());
  }

  @Test
  void read_in_doubt_tiles() {
    List<DetectedTile> detectedTiles = detectedTileRepository.findAllByJobId(JOB_ID);
    List<DetectedTile> expected =
        List.of(
            detectedTile(JOB_ID, "tile1Id", "parcel1Id", "detectedObjectId1", UNDER_MIN_CONFIDENCE),
            detectedTile(JOB_ID, "tile2Id", "parcel2Id", "detectedObjectId2", MIN_CONFIDENCE));

    List<DetectedTile> actual = subject.findInDoubtTilesByJobId(JOB_ID, detectedTiles);

    assertEquals(expected, actual.stream().peek(tile -> tile.setCreationDatetime(null)).toList());
  }
}
