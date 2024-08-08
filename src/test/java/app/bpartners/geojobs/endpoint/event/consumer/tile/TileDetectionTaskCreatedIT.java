package app.bpartners.geojobs.endpoint.event.consumer.tile;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.*;
import static app.bpartners.geojobs.repository.model.GeoJobType.PARCEL_DETECTION;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.POOL;
import static app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.DetectionType.MACHINE;
import static app.bpartners.geojobs.service.detection.DetectionResponse.REGION_CONFIDENCE_PROPERTY;
import static app.bpartners.geojobs.service.detection.DetectionResponse.REGION_LABEL_PROPERTY;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.consumer.EventConsumer;
import app.bpartners.geojobs.endpoint.event.consumer.model.ConsumableEvent;
import app.bpartners.geojobs.endpoint.event.consumer.model.TypedEvent;
import app.bpartners.geojobs.endpoint.event.model.PojaEvent;
import app.bpartners.geojobs.endpoint.event.model.tile.TileDetectionTaskCreated;
import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.repository.ParcelDetectionTaskRepository;
import app.bpartners.geojobs.repository.ParcelRepository;
import app.bpartners.geojobs.repository.ZoneTilingJobRepository;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.ParcelContent;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.*;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.detection.DetectionResponse;
import app.bpartners.geojobs.service.detection.ParcelDetectionJobService;
import app.bpartners.geojobs.service.detection.TileObjectDetector;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class TileDetectionTaskCreatedIT extends FacadeIT {
  private static final String PARCEL_DETECTION_JOB_ID = "parcelDetectionJobId";
  @Autowired EventConsumer eventConsumer;
  @Autowired ZoneTilingJobRepository ztjRepository;
  @Autowired ParcelRepository parcelRepository;
  @Autowired ParcelDetectionJobService pdjService;
  @Autowired ParcelDetectionTaskRepository pdtRepository;
  @MockBean EventProducer eventProducerMock;
  @MockBean TileObjectDetector objectsDetector;

  private static List<DetectableObjectConfiguration> detectableObjectConfiguration() {
    return List.of(
        DetectableObjectConfiguration.builder().objectType(POOL).confidence(1.0).build());
  }

  @Autowired ZoneDetectionJobService zdjService;

  @BeforeEach
  void setUp() {
    doAnswer(this::eventProducerInvocationToEventConsumer).when(eventProducerMock).accept(any());

    doReturn(aDetectionResponse(1.0, POOL)).when(objectsDetector).apply(any(), any());
  }

  @Test
  void single_event_that_succeeds() {
    var tilingJob = ztjRepository.save(aZTJ("tilingJobId", FINISHED, SUCCEEDED));
    var detectionJob = zdjService.save(aZDJ("zoneDetectionJobId", PENDING, UNKNOWN, tilingJob));
    var tile = tile("tile1Id", "bucketPath1");
    var parcel =
        parcelRepository.save(
            Parcel.builder()
                .id("parcel1Id")
                .parcelContent(ParcelContent.builder().tiles(List.of(tile)).build())
                .build());
    var tileDetectionTask =
        tileDetectionTask("tileDetectionTask1Id", PARCEL_DETECTION_JOB_ID, parcel.getId(), tile);
    var pdj =
        pdjService.create(parcelDetectionJob(PARCEL_DETECTION_JOB_ID), List.of(tileDetectionTask));
    pdtRepository.save(
        parcelDetectionTask("parcelDetectionTaskId", detectionJob.getId(), pdj.getId(), parcel));

    eventProducerMock.accept(
        List.of(
            TileDetectionTaskCreated.builder()
                .tileDetectionTask(tileDetectionTask)
                .detectableObjectConfigurations(detectableObjectConfiguration())
                .zoneDetectionJobId(detectionJob.getId())
                .build()));

    // TODO: just wait long enough for all events to be treated, then retrieve detectionJob from db
    assertTrue(detectionJob.isFinished());
  }

  private static ParcelDetectionJob parcelDetectionJob(String parcelDetectionJobId) {
    return ParcelDetectionJob.builder()
        .id(parcelDetectionJobId)
        .statusHistory(
            List.of(
                JobStatus.builder()
                    .jobId(parcelDetectionJobId)
                    .id(randomUUID().toString())
                    .creationDatetime(now())
                    .jobType(PARCEL_DETECTION)
                    .progression(PENDING)
                    .health(UNKNOWN)
                    .build()))
        .build();
  }

  Answer eventProducerInvocationToEventConsumer(InvocationOnMock invocation) {
    var consumableEvents =
        ((List) invocation.getArgument(0))
            .stream().map(argument -> toConsumableEvent((PojaEvent) argument)).toList();
    eventConsumer.accept(consumableEvents);
    return null;
  }

  ConsumableEvent toConsumableEvent(PojaEvent pojaEvent) {
    return new ConsumableEvent(
        new TypedEvent(pojaEvent.getClass().getName(), pojaEvent), () -> {}, () -> {});
  }

  private static ZoneDetectionJob aZDJ(
      String jobId,
      Status.ProgressionStatus progressionStatus,
      Status.HealthStatus healthStatus,
      ZoneTilingJob ztj) {
    return ZoneDetectionJob.builder()
        .id(jobId)
        .zoneName("dummy")
        .emailReceiver("dummy")
        .detectionType(MACHINE)
        .statusHistory(
            List.of(
                JobStatus.builder()
                    .id(randomUUID().toString())
                    .jobId(jobId)
                    .progression(progressionStatus)
                    .health(healthStatus)
                    .build()))
        .zoneTilingJob(ztj)
        .build();
  }

  private static ZoneTilingJob aZTJ(
      String jobId, Status.ProgressionStatus progressionStatus, Status.HealthStatus healthStatus) {
    return ZoneTilingJob.builder()
        .id(jobId)
        .zoneName("dummy")
        .emailReceiver("dummy")
        .statusHistory(
            List.of(
                JobStatus.builder()
                    .id(randomUUID().toString())
                    .jobId(jobId)
                    .progression(progressionStatus)
                    .health(healthStatus)
                    .build()))
        .build();
  }

  private static DetectionResponse aDetectionResponse(
      Double confidence, DetectableType detectableType) {
    double randomX = new SecureRandom().nextDouble() * 100;
    double randomY = new SecureRandom().nextDouble() * 100;
    return DetectionResponse.builder()
        .rstImageUrl("dummyImageUrl")
        .srcImageUrl("dummyImageUrl")
        .rstRaw(
            Map.of(
                "dummyRstRawProperty",
                DetectionResponse.ImageData.builder()
                    .regions(
                        Map.of(
                            "dummyRegionProperty",
                            DetectionResponse.ImageData.Region.builder()
                                .regionAttributes(
                                    Map.of(
                                        REGION_CONFIDENCE_PROPERTY,
                                        confidence.toString(),
                                        REGION_LABEL_PROPERTY,
                                        detectableType.toString()))
                                .shapeAttributes(
                                    DetectionResponse.ImageData.ShapeAttributes.builder()
                                        .allPointsX(List.of(BigDecimal.valueOf(randomX)))
                                        .allPointsY(List.of(BigDecimal.valueOf(randomY)))
                                        .build())
                                .build()))
                    .build()))
        .build();
  }

  private static TileDetectionTask tileDetectionTask(
      String id, String jobId, String parcelId, Tile tile) {
    List<TaskStatus> taskStatuses = new ArrayList<>();
    taskStatuses.add(
        TaskStatus.builder()
            .health(UNKNOWN)
            .progression(PENDING)
            .creationDatetime(now())
            .taskId(id)
            .build());

    return TileDetectionTask.builder()
        .id(id)
        .jobId(jobId)
        .parcelId(parcelId)
        .tile(tile)
        .statusHistory(taskStatuses)
        .submissionInstant(now())
        .build();
  }

  private static Tile tile(String tileId, String bucketPath) {
    return Tile.builder()
        .id(tileId)
        .creationDatetime(now())
        .bucketPath(bucketPath)
        .coordinates(new TileCoordinates().x(0).y(0).z(20))
        .build();
  }

  private static ParcelDetectionTask parcelDetectionTask(
      String taskId, String jobId, String asJobId, Parcel parcel) {
    List<TaskStatus> taskStatuses = new ArrayList<>();
    taskStatuses.add(
        TaskStatus.builder()
            .progression(PROCESSING)
            .health(UNKNOWN)
            .creationDatetime(now())
            .taskId(taskId)
            .build());
    return ParcelDetectionTask.builder()
        .id(taskId)
        .jobId(jobId)
        .asJobId(asJobId)
        .submissionInstant(now())
        .parcels(List.of(parcel))
        .statusHistory(taskStatuses)
        .build();
  }
}
