package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.endpoint.rest.controller.ZoneDetectionJobControllerIT.someDetectionTask;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING;
import static app.bpartners.geojobs.repository.model.GeoJobType.DETECTION;
import static app.bpartners.geojobs.service.ParcelDetectionTaskServiceIT.detectedTile;
import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.repository.*;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.ParcelContent;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.*;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.detection.DetectionMapper;
import app.bpartners.geojobs.service.detection.DetectionResponse;
import app.bpartners.geojobs.service.detection.TileObjectDetector;
import java.time.Instant;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class ParcelDetectionTaskConsumerIT extends FacadeIT {
  public static final String JOB_ID = "JOB_ID";
  public static final String DETECTION_TASK_ID = "detection_task1";
  public static final String PARCEL_ID = "parcelId";
  @MockBean TileObjectDetector objectDetector;
  @MockBean DetectionMapper detectionMapper;
  @MockBean EventProducer eventProducer;
  @MockBean TileDetectionTaskRepository tileDetectionTaskRepository;
  @Autowired ParcelDetectionTaskConsumer subject;
  @Autowired DetectableObjectConfigurationRepository objectConfigurationRepository;
  @Autowired ParcelDetectionTaskRepository parcelDetectionTaskRepository;
  @Autowired ZoneDetectionJobRepository jobRepository;
  @Autowired DetectedTileRepository detectedTileRepository;
  @Autowired ParcelRepository parcelRepository;

  private static ParcelDetectionTask detectionTask() {
    List<Parcel> parcels = getParcels();
    return ParcelDetectionTask.builder()
        .id(DETECTION_TASK_ID)
        .jobId(JOB_ID)
        .parcels(parcels)
        .statusHistory(
            List.of(
                TaskStatus.builder()
                    .id(randomUUID().toString())
                    .progression(PENDING)
                    .jobType(DETECTION)
                    .health(UNKNOWN)
                    .build()))
        .submissionInstant(Instant.now())
        .build();
  }

  @NotNull
  private static List<Parcel> getParcels() {
    return List.of(
        Parcel.builder()
            .id("parcelId")
            .parcelContent(ParcelContent.builder().tiles(List.of(new Tile())).build())
            .build());
  }

  @BeforeEach
  void setUp() {
    when(objectDetector.apply(any(), any())).thenReturn(DetectionResponse.builder().build());
    when(detectionMapper.toDetectedTile(any(), any(), any(), any(), any()))
        .thenReturn(someDetectedTile());
    when(tileDetectionTaskRepository.saveAll(any())).thenReturn(List.of(new TileDetectionTask()));
    jobRepository.save(ZoneDetectionJob.builder().id(JOB_ID).build());
    parcelRepository.saveAll(getParcels());
    parcelDetectionTaskRepository.save(detectionTask());
    objectConfigurationRepository.save(
        DetectableObjectConfiguration.builder()
            .id("detectableObjectConfigurationId1")
            .confidence(0.70)
            .objectType(DetectableType.TOITURE_REVETEMENT)
            .detectionJobId(JOB_ID)
            .build());
  }

  @Test
  void accept_ok() {
    subject.accept(someDetectionTask(JOB_ID, DETECTION_TASK_ID, PARCEL_ID));

    var eventsCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducer, times(detectionTask().getParcels().size()))
        .accept(eventsCaptor.capture());
  }

  private static MachineDetectedTile someDetectedTile() {
    return detectedTile(JOB_ID, "tileId", "parcel1Id", "detectedObject1Id", 0.98);
  }
}
