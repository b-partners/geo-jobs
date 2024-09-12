package app.bpartners.geojobs.endpoint.event.consumer.tile;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.*;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.POOL;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.status.ParcelDetectionStatusRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.status.ZDJParcelsStatusRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.status.ZDJStatusRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.tile.TileDetectionTaskCreated;
import app.bpartners.geojobs.job.model.*;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.*;
import app.bpartners.geojobs.service.AnnotationRetrievingJobService;
import app.bpartners.geojobs.service.event.ZoneDetectionJobAnnotationProcessor;
import app.bpartners.geojobs.sqs.EventProducerInvocationMock;
import app.bpartners.geojobs.sqs.LocalEventQueue;
import app.bpartners.geojobs.utils.detection.*;
import app.bpartners.geojobs.utils.detection.DetectionIT;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

@Slf4j
class TileDetectionTaskCreatedIT extends DetectionIT {
  private static final double OBJECT_DETECTION_SUCCESS_RATE = 75.0;
  private static final int DEFAULT_EVENT_DELAY_SPEED_FACTOR = 10;
  private static final double MOCK_DETECTION_RESPONSE_CONFIDENCE = 1.0;
  @MockBean protected ZoneDetectionJobAnnotationProcessor jobAnnotationProcessorMock;
  @MockBean protected AnnotationRetrievingJobService annotationRetrievingJobServiceMock;
  @Autowired LocalEventQueue localEventQueue;
  @MockBean EventProducer eventProducerMock;
  EventProducerInvocationMock eventProducerInvocationMock = new EventProducerInvocationMock();

  @NonNull
  private static List<LocalEventQueue.CustomEventDelayConfig> customEventConfigList() {
    return List.of(
        new LocalEventQueue.CustomEventDelayConfig(
            ParcelDetectionStatusRecomputingSubmitted.class, 50),
        new LocalEventQueue.CustomEventDelayConfig(ZDJParcelsStatusRecomputingSubmitted.class, 50),
        new LocalEventQueue.CustomEventDelayConfig(ZDJStatusRecomputingSubmitted.class, 50));
  }

  private static List<DetectableObjectConfiguration> detectableObjectConfiguration() {
    return List.of(
        DetectableObjectConfiguration.builder()
            .objectType(POOL)
            .confidence(MOCK_DETECTION_RESPONSE_CONFIDENCE)
            .build());
  }

  @BeforeEach
  void setUp() {
    localEventQueue.configure(customEventConfigList(), DEFAULT_EVENT_DELAY_SPEED_FACTOR);
    doAnswer(
            invocationOnMock ->
                eventProducerInvocationMock.apply(localEventQueue, invocationOnMock))
        .when(eventProducerMock)
        .accept(any());
    when(jobAnnotationServiceMock.processAnnotationJob(any(), any())).thenReturn(null);
    when(annotationRetrievingJobServiceMock.findAllByDetectionJobId(any())).thenReturn(List.of());
    doNothing().when(mailerMock).accept(any());
    when(jobAnnotationProcessorMock.accept(any(), any(), any(), any(), any(), any()))
        .thenReturn(ZoneDetectionJobAnnotationProcessor.AnnotationJobIds.builder().build());
    new ObjectsDetectorMockResponse(objectsDetectorMock)
        .apply(MOCK_DETECTION_RESPONSE_CONFIDENCE, POOL, OBJECT_DETECTION_SUCCESS_RATE);
  }

  @SneakyThrows
  @Test
  void single_event_that_succeeds() {
    SingleEventDataSetUp testData = getSingleEventDataSetUp();

    eventProducerMock.accept(
        List.of(
            TileDetectionTaskCreated.builder()
                .tileDetectionTask(testData.tileDetectionTask())
                .detectableObjectConfigurations(detectableObjectConfiguration())
                .zoneDetectionJobId(testData.detectionJob().getId())
                .build()));

    eventProducerMock.accept(
        List.of(new ZDJParcelsStatusRecomputingSubmitted(testData.detectionJobId())));
    eventProducerMock.accept(List.of(new ZDJStatusRecomputingSubmitted(testData.detectionJobId())));
    Thread.sleep(Duration.ofSeconds(15L));
    if (localEventQueue != null) localEventQueue.attemptSchedulerShutDown();
    var retrievedJob = zdjService.findById(testData.detectionJob().getId());

    assertTrue(retrievedJob.isSucceeded());
  }

  @SneakyThrows
  @Test
  @Disabled("TODO: fail")
  void thousand_events_that_succeeds() {
    ThousandEventDataSetUp testData = getThousandEventDataSetUp();

    testData
        .parcelDetectionJobWithTasks()
        .forEach(
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
                                  .zoneDetectionJobId(testData.detectionJobId())
                                  .build())));
            });

    eventProducerMock.accept(
        List.of(new ZDJParcelsStatusRecomputingSubmitted(testData.detectionJobId())));
    eventProducerMock.accept(List.of(new ZDJStatusRecomputingSubmitted(testData.detectionJobId())));
    Thread.sleep(Duration.ofSeconds(450L));
    if (localEventQueue != null) localEventQueue.attemptSchedulerShutDown();

    var retrievedJob = zdjService.findById(testData.detectionJob().getId());
    var actualPDJRecords =
        testData.parcelDetectionJobWithTasks().stream()
            .map(
                record ->
                    new PDJRecord(
                        parcelDetectionTaskRepository
                            .findById(record.parcelDetectionTask.getId())
                            .orElseThrow(),
                        pdjService.findById(record.parcelDetectionJob.getId()),
                        List.of()))
            .toList();
    var updatedPDJ = actualPDJRecords.stream().map(PDJRecord::parcelDetectionJob).toList();
    var updatedPDT = actualPDJRecords.stream().map(PDJRecord::parcelDetectionTask).toList();
    assertTrue(updatedPDJ.stream().allMatch(Job::isSucceeded));
    assertTrue(updatedPDT.stream().allMatch(Task::isSucceeded));
    assertTrue(retrievedJob.isSucceeded());
  }

  @NonNull
  private ThousandEventDataSetUp getThousandEventDataSetUp() {
    String tilingJobId = randomUUID().toString();
    var tilingJob = ztjRepository.save(finishedZoneTilingJob(tilingJobId));
    String detectionJobId = randomUUID().toString();
    var detectionJob = zdjService.save(processingZoneDetectionJob(detectionJobId, tilingJob));
    var parcelDetectionTasks =
        parcelDetectionTaskRepository.saveAll(thousandTilesInFiftyParcels(detectionJobId));
    var parcelDetectionJobWithTasks = someParcelDetectionJobWithTask(parcelDetectionTasks);
    return new ThousandEventDataSetUp(detectionJobId, detectionJob, parcelDetectionJobWithTasks);
  }

  private record ThousandEventDataSetUp(
      String detectionJobId,
      ZoneDetectionJob detectionJob,
      List<PDJRecord> parcelDetectionJobWithTasks) {}

  private void processParcelDetectionData(
      PDJRecord parcelDetectionRecord, List<TileDetectionTask> tileDetectionTasks) {
    var parcelDetectionJob = parcelDetectionRecord.parcelDetectionJob;
    var parcelDetectionTask = parcelDetectionRecord.parcelDetectionTask;
    var parcelDetectionJobId = parcelDetectionJob.getId();

    parcelDetectionTask.setAsJobId(parcelDetectionJobId);
    this.parcelDetectionTaskRepository.save(parcelDetectionTask);

    tileDetectionTasks.forEach(
        tileDetectionTask -> tileDetectionTask.setJobId(parcelDetectionJobId));
    pdjService.create(parcelDetectionJob, tileDetectionTasks);
  }

  @NonNull
  private SingleEventDataSetUp getSingleEventDataSetUp() {
    String tilingJobId = randomUUID().toString();
    String detectionJobId = randomUUID().toString();
    String parcelDetectionJobId = randomUUID().toString();
    var tilingJob = ztjRepository.save(finishedZoneTilingJob(tilingJobId));
    var detectionJob = zdjService.save(processingZoneDetectionJob(detectionJobId, tilingJob));
    var parcel =
        parcelRepository.save(
            parcelCreator.create(
                randomUUID().toString(),
                List.of(tileCreator.create(randomUUID().toString(), "bucketPath"))));
    var tileDetectionTask =
        tileDetectionTaskCreator.create(
            randomUUID().toString(),
            parcelDetectionJobId,
            parcel.getId(),
            parcel.getParcelContent().getFirstTile(),
            PENDING,
            UNKNOWN);
    var parcelDetectionJob =
        parcelDetectionJobCreator.create(parcelDetectionJobId, PENDING, UNKNOWN);
    var savedPDJ = pdjService.create(parcelDetectionJob, List.of(tileDetectionTask));
    var processingParcelDetectionTask =
        parcelDetectionTaskCreator.create(
            randomUUID().toString(),
            detectionJob.getId(),
            savedPDJ.getId(),
            parcel,
            PROCESSING,
            UNKNOWN);
    parcelDetectionTaskRepository.save(processingParcelDetectionTask);
    return new SingleEventDataSetUp(detectionJobId, detectionJob, tileDetectionTask);
  }

  private record SingleEventDataSetUp(
      String detectionJobId, ZoneDetectionJob detectionJob, TileDetectionTask tileDetectionTask) {}

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

  private List<ParcelDetectionTask> thousandTilesInFiftyParcels(String jobId) {
    var parcelDetectionTasks = new ArrayList<ParcelDetectionTask>();
    for (int i = 0; i < 50; i++) {
      var savedParcel = parcelRepository.save(parcelCreator.create(20));
      String taskId = randomUUID().toString();
      String asJobId = null;
      parcelDetectionTasks.add(
          parcelDetectionTaskCreator.create(
              taskId, jobId, asJobId, savedParcel, PROCESSING, UNKNOWN));
    }
    return parcelDetectionTasks;
  }

  record PDJRecord(
      ParcelDetectionTask parcelDetectionTask,
      ParcelDetectionJob parcelDetectionJob,
      List<TileDetectionTask> tileDetectionTasks) {}
}
