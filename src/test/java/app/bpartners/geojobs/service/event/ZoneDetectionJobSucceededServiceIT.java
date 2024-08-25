package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.service.ParcelDetectionTaskServiceIT.detectedTile;
import static org.junit.jupiter.api.Assertions.*;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.zone.ZoneDetectionJobSucceeded;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.repository.*;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.detection.*;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class ZoneDetectionJobSucceededServiceIT extends FacadeIT {
  public static final String SUCCEEDED_JOB_ID = "succeededJobId";
  public static final String HUMAN_ZDJ_ID = "humanZdjId";
  public static final String SUCCEEDED_JOB_ID_2 = "succeededJobId2";
  public static final String HUMAN_ZDJ_ID_2 = "humanZdjId2";
  public static final String ZONE_TILING_JOB_ID = "zoneTilingJobId";
  public static final String ZONE_TILING_JOB_ID_2 = "zoneTilingJobId2";
  @MockBean EventProducer eventProducer;
  @Autowired ZoneDetectionJobSucceededService subject;
  @Autowired private ZoneDetectionJobRepository jobRepository;
  @Autowired private DetectedTileRepository detectedTileRepository;
  @Autowired private DetectableObjectConfigurationRepository objectConfigurationRepository;
  @Autowired private HumanDetectionJobRepository humanDetectionJobRepository;
  @Autowired private ParcelDetectionTaskRepository parcelDetectionTaskRepository;
  @Autowired private ParcelRepository parcelRepository;
  @MockBean ZoneDetectionJobAnnotationProcessor zoneDetectionJobAnnotationProcessorMock;

  @BeforeEach
  void setUp() {
    jobRepository.saveAll(
        List.of(
            ZoneDetectionJob.builder()
                .id(SUCCEEDED_JOB_ID)
                .detectionType(ZoneDetectionJob.DetectionType.MACHINE)
                .zoneTilingJob(
                    ZoneTilingJob.builder()
                        .id(ZONE_TILING_JOB_ID)
                        .zoneName("dummy")
                        .emailReceiver("dummy")
                        .build())
                .build(),
            ZoneDetectionJob.builder()
                .id(SUCCEEDED_JOB_ID_2)
                .detectionType(ZoneDetectionJob.DetectionType.MACHINE)
                .zoneTilingJob(
                    ZoneTilingJob.builder()
                        .id(ZONE_TILING_JOB_ID_2)
                        .zoneName("dummy")
                        .emailReceiver("dummy")
                        .build())
                .build(),
            ZoneDetectionJob.builder()
                .id(HUMAN_ZDJ_ID)
                .detectionType(ZoneDetectionJob.DetectionType.HUMAN)
                .zoneTilingJob(
                    ZoneTilingJob.builder()
                        .id(ZONE_TILING_JOB_ID)
                        .zoneName("dummy")
                        .emailReceiver("dummy")
                        .build())
                .build(),
            ZoneDetectionJob.builder()
                .id(HUMAN_ZDJ_ID_2)
                .zoneTilingJob(
                    ZoneTilingJob.builder()
                        .id(ZONE_TILING_JOB_ID_2)
                        .zoneName("dummy")
                        .emailReceiver("dummy")
                        .build())
                .detectionType(ZoneDetectionJob.DetectionType.HUMAN)
                .build()));
    parcelRepository.saveAll(getJob1Parcels());
    parcelRepository.saveAll(getJob2Parcels());
    parcelDetectionTaskRepository.saveAll(
        List.of(
            ParcelDetectionTask.builder()
                .id("detectionTaskId")
                .jobId(SUCCEEDED_JOB_ID)
                .parcels(getJob1Parcels())
                .build(),
            ParcelDetectionTask.builder()
                .id("detectionTaskId2")
                .jobId(SUCCEEDED_JOB_ID_2)
                .parcels(getJob2Parcels())
                .build()));
    detectedTileRepository.saveAll(
        List.of(
            detectedTile(SUCCEEDED_JOB_ID, "tile2Id", "parcel2Id", "detectedObjectId2", 0.5),
            detectedTile(SUCCEEDED_JOB_ID, "tile1Id", "parcel1Id", "detectedObjectId1", 0.8),
            detectedTile(SUCCEEDED_JOB_ID_2, "tile3Id", "parcel3Id", "detectedObjectId3", 0.8)));
    objectConfigurationRepository.saveAll(
        List.of(
            DetectableObjectConfiguration.builder()
                .id("detectableObjectConfigurationId")
                .confidence(0.8)
                .objectType(DetectableType.ROOF)
                .detectionJobId(SUCCEEDED_JOB_ID)
                .build(),
            DetectableObjectConfiguration.builder()
                .id("detectableObjectConfigurationId2")
                .confidence(0.8)
                .objectType(DetectableType.ROOF)
                .detectionJobId(SUCCEEDED_JOB_ID_2)
                .build()));
  }

  @NotNull
  private static List<Parcel> getJob2Parcels() {
    return List.of(Parcel.builder().id("parcel3Id").build());
  }

  @NotNull
  private static List<Parcel> getJob1Parcels() {
    return List.of(
        Parcel.builder().id("parcel1Id").build(), Parcel.builder().id("parcel2Id").build());
  }

  @Test
  @Disabled("TODO: mock annotator jobsApi")
  void handle_human_detection_job() {
    var humanDetectionBefore = humanDetectionJobRepository.findAll();
    var humanZDJ1Before = jobRepository.findById(HUMAN_ZDJ_ID).orElseThrow();

    subject.accept(new ZoneDetectionJobSucceeded(SUCCEEDED_JOB_ID));
    subject.accept(new ZoneDetectionJobSucceeded(SUCCEEDED_JOB_ID_2));

    var humanZDJ1After = jobRepository.findById(HUMAN_ZDJ_ID).orElseThrow();
    var humanZDJ2After = jobRepository.findById(HUMAN_ZDJ_ID_2).orElseThrow();
    var humanDetectionAfter = humanDetectionJobRepository.findByZoneDetectionJobId(HUMAN_ZDJ_ID);
    var humanDetectionAfter2 = humanDetectionJobRepository.findByZoneDetectionJobId(HUMAN_ZDJ_ID_2);
    var humanZDJ1BeforeStatus = humanZDJ1Before.getStatus();
    var humanZDJ2AfterStatus = humanZDJ2After.getStatus();
    assertTrue(humanDetectionBefore.isEmpty());
    assertTrue(!humanDetectionAfter.isEmpty());
    assertEquals(humanZDJ1Before, humanZDJ1After);
    assertEquals(
        JobStatus.builder()
            .id(humanZDJ1BeforeStatus.getId())
            .progression(Status.ProgressionStatus.PENDING)
            .health(Status.HealthStatus.UNKNOWN)
            .creationDatetime(humanZDJ1BeforeStatus.getCreationDatetime())
            .build(),
        humanZDJ1BeforeStatus);
    assertEquals(
        JobStatus.builder()
            .id(humanZDJ2AfterStatus.getId())
            .progression(Status.ProgressionStatus.PENDING)
            .health(Status.HealthStatus.UNKNOWN)
            .creationDatetime(humanZDJ2AfterStatus.getCreationDatetime())
            .build(),
        humanZDJ2AfterStatus);
    assertEquals(HUMAN_ZDJ_ID, humanDetectionAfter.getFirst().getZoneDetectionJobId());
    assertFalse(humanDetectionAfter.getFirst().getMachineDetectedTiles().isEmpty());
    assertFalse(humanDetectionAfter2.getFirst().getMachineDetectedTiles().isEmpty());
  }
}
