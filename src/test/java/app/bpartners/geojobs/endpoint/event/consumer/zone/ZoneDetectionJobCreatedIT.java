package app.bpartners.geojobs.endpoint.event.consumer.zone;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.POOL;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.AutoTaskStatisticRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.status.ParcelDetectionStatusRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.status.ZDJStatusRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.zone.ZoneDetectionJobCreated;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.service.event.ZoneDetectionJobAnnotationProcessor;
import app.bpartners.geojobs.sqs.EventProducerInvocationMock;
import app.bpartners.geojobs.sqs.LocalEventQueue;
import app.bpartners.geojobs.utils.detection.DetectionIT;
import app.bpartners.geojobs.utils.detection.ObjectsDetectorMockResponse;
import java.time.Duration;
import java.util.List;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class ZoneDetectionJobCreatedIT extends DetectionIT {
  private static final double OBJECT_DETECTION_SUCCESS_RATE = 100.0;
  private static final int DEFAULT_EVENT_DELAY_SPEED_FACTOR = 10;
  private static final double MOCK_DETECTION_RESPONSE_CONFIDENCE = 1.0;
  @Autowired LocalEventQueue localEventQueue;
  @MockBean EventProducer eventProducerMock;
  EventProducerInvocationMock eventProducerInvocationMock = new EventProducerInvocationMock();

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
    when(jobAnnotationProcessorMock.accept(any(), any(), any(), any(), any()))
        .thenReturn(ZoneDetectionJobAnnotationProcessor.AnnotationJobIds.builder().build());
    new ObjectsDetectorMockResponse(objectsDetectorMock)
        .apply(MOCK_DETECTION_RESPONSE_CONFIDENCE, POOL, OBJECT_DETECTION_SUCCESS_RATE);
  }

  @NonNull
  private static List<LocalEventQueue.CustomEventDelayConfig> customEventConfigList() {
    return List.of(
        new LocalEventQueue.CustomEventDelayConfig(AutoTaskStatisticRecomputingSubmitted.class, 50),
        new LocalEventQueue.CustomEventDelayConfig(
            ParcelDetectionStatusRecomputingSubmitted.class, 50),
        new LocalEventQueue.CustomEventDelayConfig(ZDJStatusRecomputingSubmitted.class, 50));
  }

  @SneakyThrows
  @Test
  void zdj_created_triggered_with_single_parcel_and_zdj_succeeds() {
    var detectionJob = someDetectionJob();

    eventProducerMock.accept(
        List.of(ZoneDetectionJobCreated.builder().zoneDetectionJob(detectionJob).build()));

    Thread.sleep(Duration.ofSeconds(30L));
    if (localEventQueue != null) localEventQueue.attemptSchedulerShutDown();

    var actualJob = zdjService.findById(detectionJob.getId());
    assertTrue(actualJob.isSucceeded());
  }

  @NonNull
  private ZoneDetectionJob someDetectionJob() {
    String tilingJobId = randomUUID().toString();
    String detectionJobId = randomUUID().toString();
    var tilingJob = ztjRepository.save(finishedZoneTilingJob(tilingJobId));
    var detectionJob = zdjService.save(pendingZoneDetectionJob(detectionJobId, tilingJob));
    var parcel =
        parcelRepository.save(
            parcelCreator.create(
                randomUUID().toString(),
                List.of(tileCreator.create(randomUUID().toString(), "bucketPath"))));
    parcelDetectionTaskRepository.save(
        parcelDetectionTaskCreator.create(
            randomUUID().toString(), detectionJob.getId(), null, parcel, PENDING, UNKNOWN));
    return detectionJob;
  }
}
