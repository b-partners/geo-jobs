package app.bpartners.geojobs.endpoint.event.consumer.tile;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.*;
import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
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

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.consumer.EventConsumer;
import app.bpartners.geojobs.endpoint.event.consumer.model.ConsumableEvent;
import app.bpartners.geojobs.endpoint.event.consumer.model.TypedEvent;
import app.bpartners.geojobs.endpoint.event.model.AutoTaskStatisticRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.PojaEvent;
import app.bpartners.geojobs.endpoint.event.model.status.ZDJParcelsStatusRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.status.ZDJStatusRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.tile.TileDetectionTaskCreated;
import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.repository.*;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.ParcelContent;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.*;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.TaskToJobConverter;
import app.bpartners.geojobs.service.detection.DetectionResponse;
import app.bpartners.geojobs.service.detection.ParcelDetectionJobService;
import app.bpartners.geojobs.service.detection.TileObjectDetector;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.sqs.LocalSQSTrigger;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

@Slf4j
class TileDetectionTaskCreatedIT extends FacadeIT {
  // TODO: set the IDs randomly
  private static final String PARCEL_DETECTION_JOB_ID =
      "parcelDetectionJobIdForTileDetectionTaskCreatedIT";
  public static final String ZONE_DETECTION_JOB_ID =
      "zoneDetectionJobIdForTileDetectionTaskCreatedIT";
  public static final String TILING_JOB_ID = "tilingJobIdForTileDetectionTaskCreatedIT";
  private static final String PARCEL_DETECTION_JOB_ID2 =
      "parcelDetectionJobIdForTileDetectionTaskCreatedIT2";
  public static final String ZONE_DETECTION_JOB_ID2 =
      "zoneDetectionJobIdForTileDetectionTaskCreatedIT2";
  public static final String TILING_JOB_ID2 = "tilingJobIdForTileDetectionTaskCreatedIT2";
  public static final double OBJECT_DETECTION_SUCCESS_RATE = 65.0;
  @Autowired EventConsumer eventConsumer;
  @Autowired ZoneTilingJobRepository ztjRepository;
  @Autowired ParcelRepository parcelRepository;
  @Autowired ParcelDetectionJobService pdjService;
  @Autowired ParcelDetectionTaskRepository pdtRepository;
  @MockBean TileObjectDetector objectsDetector;
  @Autowired ZoneDetectionJobService zdjService;
  @Autowired TaskToJobConverter<ParcelDetectionTask, ParcelDetectionJob> taskToJobConverter;
  @Autowired ZoneDetectionJobRepository zdjRepository;
  @Autowired ParcelDetectionJobRepository pdjRepository;
  @MockBean EventProducer eventProducerMock;
  final LocalSQSTrigger localSQSTrigger = new LocalSQSTrigger();

  private static List<DetectableObjectConfiguration> detectableObjectConfiguration() {
    return List.of(
        DetectableObjectConfiguration.builder().objectType(POOL).confidence(1.0).build());
  }

  @BeforeEach
  void setUp() {
    doAnswer(this::eventProducerInvocationToEventConsumer).when(eventProducerMock).accept(any());

    configureRandomObjectsDetectorResponse(OBJECT_DETECTION_SUCCESS_RATE);
  }

  @SneakyThrows
  @Test
  void single_event_that_succeeds() {
    var tilingJob = ztjRepository.save(aZTJ(TILING_JOB_ID, FINISHED, SUCCEEDED));
    var detectionJob = zdjService.save(aZDJ(ZONE_DETECTION_JOB_ID, PENDING, UNKNOWN, tilingJob));
    var parcel = parcelRepository.save(parcel("parcel1Id", tile("tile1Id", "bucketPath1")));
    var tileDetectionTask =
        tileDetectionTask(
            "tileDetectionTask1Id",
            PARCEL_DETECTION_JOB_ID,
            parcel.getId(),
            parcel.getParcelContent().getFirstTile());
    var parcelDetectionJob =
        pdjService.create(parcelDetectionJob(PARCEL_DETECTION_JOB_ID), List.of(tileDetectionTask));
    pdtRepository.save(
        parcelDetectionTask(
            "parcelDetectionTaskId", detectionJob.getId(), parcelDetectionJob.getId(), parcel));

    eventProducerMock.accept(
        List.of(
            TileDetectionTaskCreated.builder()
                .tileDetectionTask(tileDetectionTask)
                .detectableObjectConfigurations(detectableObjectConfiguration())
                .zoneDetectionJobId(detectionJob.getId())
                .build()));

    eventProducerMock.accept(
        List.of(new ZDJParcelsStatusRecomputingSubmitted(ZONE_DETECTION_JOB_ID)));
    eventProducerMock.accept(List.of(new ZDJStatusRecomputingSubmitted(ZONE_DETECTION_JOB_ID)));
    eventProducerMock.accept(
        List.of(new AutoTaskStatisticRecomputingSubmitted(ZONE_DETECTION_JOB_ID)));

    Thread.sleep(Duration.ofSeconds(15L));
    var retrievedJob = zdjService.findById(detectionJob.getId());

    assertTrue(retrievedJob.isFinished());
  }

  @SneakyThrows
  @Test
  void thousand_events_that_succeeds() {
    var tilingJob = ztjRepository.save(aZTJ(TILING_JOB_ID2, FINISHED, SUCCEEDED));
    var detectionJob = zdjService.save(aZDJ(ZONE_DETECTION_JOB_ID2, PENDING, UNKNOWN, tilingJob));

    var parcelDetectionTasks =
        pdtRepository.saveAll(someParcelDetectionTask(ZONE_DETECTION_JOB_ID2, 50, 20));
    var parcelDetectionJobWithTasks = someParcelDetectionJobWithTask(parcelDetectionTasks);

    eventProducerMock.accept(
        List.of(new ZDJParcelsStatusRecomputingSubmitted(ZONE_DETECTION_JOB_ID2)));
    eventProducerMock.accept(List.of(new ZDJStatusRecomputingSubmitted(ZONE_DETECTION_JOB_ID2)));
    eventProducerMock.accept(
        List.of(new AutoTaskStatisticRecomputingSubmitted(ZONE_DETECTION_JOB_ID2)));

    parcelDetectionJobWithTasks.forEach(
        record -> {
          var tileDetectionTasks = record.tileDetectionTasks;
          processParcelDetectionData(record, tileDetectionTasks);

          tileDetectionTasks.forEach(
              tileDetectionTask ->
                  eventProducerMock.accept(
                      List.of(
                          TileDetectionTaskCreated.builder()
                              .tileDetectionTask(tileDetectionTask)
                              .detectableObjectConfigurations(detectableObjectConfiguration())
                              .zoneDetectionJobId(ZONE_DETECTION_JOB_ID2)
                              .build())));
        });

    Thread.sleep(Duration.ofSeconds(60L));
    localSQSTrigger.shutDownScheduler();
    var retrievedJob = zdjService.findById(detectionJob.getId());

    assertTrue(retrievedJob.isFinished());
  }

  private void processParcelDetectionData(
      PDJRecord parcelDetectionRecord, List<TileDetectionTask> tileDetectionTasks) {
    var parcelDetectionJob = parcelDetectionRecord.parcelDetectionJob;
    var parcelDetectionTask = parcelDetectionRecord.parcelDetectionTask;
    var parcelDetectionJobId = parcelDetectionJob.getId();

    parcelDetectionTask.setAsJobId(parcelDetectionJobId);
    pdtRepository.save(parcelDetectionTask);

    tileDetectionTasks.forEach(
        tileDetectionTask -> tileDetectionTask.setJobId(parcelDetectionJobId));
    pdjService.create(parcelDetectionJob, tileDetectionTasks);
  }

  private List<PDJRecord> someParcelDetectionJobWithTask(
      List<ParcelDetectionTask> parcelDetectionTasks) {
    List<PDJRecord> pdjWithTasks = new ArrayList<>();
    for (ParcelDetectionTask task : parcelDetectionTasks) {
      ParcelDetectionJob parcelDetectionJob = taskToJobConverter.apply(task);
      var tileDetectionTasks =
          task.getParcel().getParcelContent().getTiles().stream()
              .map(tile -> taskToJobConverter.apply(task, tile))
              .toList();
      pdjWithTasks.add(new PDJRecord(task, parcelDetectionJob, tileDetectionTasks));
    }
    return pdjWithTasks;
  }

  private List<ParcelDetectionTask> someParcelDetectionTask(
      String jobId, int nbParcelDetectionTask, int nbTilePerParcel) {
    if (nbParcelDetectionTask < 0)
      throw new RuntimeException(
          "nbParcelDetectionTask must be > 0 but was " + nbParcelDetectionTask);
    var parcelDetectionTasks = new ArrayList<ParcelDetectionTask>();
    for (int i = 0; i < nbParcelDetectionTask; i++) {
      var savedParcel = parcelRepository.save(someParcel(nbTilePerParcel));
      String taskId = randomUUID().toString();
      String asJobId = null;
      parcelDetectionTasks.add(parcelDetectionTask(taskId, jobId, asJobId, savedParcel));
    }
    return parcelDetectionTasks;
  }

  private Parcel someParcel(int nbTilePerParcel) {
    if (nbTilePerParcel < 0)
      throw new RuntimeException("nbTilePerParcel must be > 0 but was " + nbTilePerParcel);
    var tiles = new ArrayList<Tile>();
    for (int j = 0; j < nbTilePerParcel; j++) {
      var tileRow = (j + 1);
      tiles.add(tile("tileId" + tileRow, "bucketPath" + tileRow));
    }
    return parcel(randomUUID().toString(), tiles);
  }

  private static Parcel parcel(String id, List<Tile> tiles) {
    return Parcel.builder()
        .id(id)
        .parcelContent(ParcelContent.builder().tiles(tiles).build())
        .build();
  }

  private static Parcel parcel(String id, Tile tile) {
    return parcel(id, List.of(tile));
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

  void configureRandomObjectsDetectorResponse(double successRate) {
    Random random = new Random();
    doAnswer(
            invocation -> {
              double randomDouble = random.nextDouble() * 100;
              if (randomDouble < successRate) {
                return aDetectionResponse(1.0, POOL);
              } else {
                throw new ApiException(SERVER_EXCEPTION, "Server error");
              }
            })
        .when(objectsDetector)
        .apply(any(), any());
  }

  Answer eventProducerInvocationToEventConsumer(InvocationOnMock invocation) {
    var consumableEvents =
        ((List) invocation.getArgument(0))
            .stream().map(argument -> toConsumableEvent((PojaEvent) argument)).toList();

    consumableEvents.forEach(
        consumableEvent -> {
          PojaEvent pojaEvent = ((ConsumableEvent) consumableEvent).getEvent().payload();
          Runnable runnable =
              () -> {
                try {
                  eventConsumer.accept(List.of((ConsumableEvent) consumableEvent));
                } catch (Exception e) {
                  throw new RuntimeException(e);
                }
              };
          localSQSTrigger.handle(runnable, pojaEvent);
        });
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
                    .creationDatetime(now())
                    .build()))
        .zoneTilingJob(ztj)
        .submissionInstant(now())
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
                    .creationDatetime(now())
                    .build()))
        .submissionInstant(now())
        .build();
  }

  private DetectionResponse aDetectionResponse(Double confidence, DetectableType detectableType) {
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

  private TileDetectionTask tileDetectionTask(String id, String jobId, String parcelId, Tile tile) {
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

  private Tile tile(String tileId, String bucketPath) {
    return Tile.builder()
        .id(tileId)
        .creationDatetime(now())
        .bucketPath(bucketPath)
        .coordinates(new TileCoordinates().x(0).y(0).z(20))
        .build();
  }

  private ParcelDetectionTask parcelDetectionTask(
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

  record PDJRecord(
      ParcelDetectionTask parcelDetectionTask,
      ParcelDetectionJob parcelDetectionJob,
      List<TileDetectionTask> tileDetectionTasks) {}
}
