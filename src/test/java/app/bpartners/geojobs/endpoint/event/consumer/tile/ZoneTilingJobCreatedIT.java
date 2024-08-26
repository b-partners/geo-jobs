package app.bpartners.geojobs.endpoint.event.consumer.tile;

import static app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType.ROOF;
import static app.bpartners.geojobs.file.hash.FileHashAlgorithm.SHA256;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.POOL;
import static app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.DetectionType.MACHINE;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.AutoTaskStatisticRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.status.ParcelDetectionStatusRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.status.ZDJStatusRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.status.ZTJStatusRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.zone.ZoneTilingJobCreated;
import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectConfiguration;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.file.hash.FileHash;
import app.bpartners.geojobs.repository.FullDetectionRepository;
import app.bpartners.geojobs.repository.TilingTaskRepository;
import app.bpartners.geojobs.repository.model.detection.FullDetection;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.JobFinishedMailer;
import app.bpartners.geojobs.service.event.ZoneDetectionJobAnnotationProcessor;
import app.bpartners.geojobs.service.tiling.downloader.TilesDownloader;
import app.bpartners.geojobs.sqs.EventProducerInvocationMock;
import app.bpartners.geojobs.sqs.LocalEventQueue;
import app.bpartners.geojobs.utils.detection.DetectionIT;
import app.bpartners.geojobs.utils.detection.ObjectsDetectorMockResponse;
import app.bpartners.geojobs.utils.tiling.TilingTaskCreator;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

@Slf4j
class ZoneTilingJobCreatedIT extends DetectionIT {
  private static final double OBJECT_DETECTION_SUCCESS_RATE = 100.0;
  private static final int DEFAULT_EVENT_DELAY_SPEED_FACTOR = 10;
  private static final double MOCK_DETECTION_RESPONSE_CONFIDENCE = 1.0;
  @Autowired FullDetectionRepository fullDetectionRepository;
  @Autowired TilingTaskRepository tilingTaskRepository;
  @Autowired LocalEventQueue localEventQueue;
  @MockBean EventProducer eventProducerMock;
  @MockBean TilesDownloader tilesDownloaderMock;
  @MockBean BucketComponent bucketComponentMock;
  @MockBean protected JobFinishedMailer<ZoneTilingJob> tilingJobMailerMock;

  EventProducerInvocationMock eventProducerInvocationMock = new EventProducerInvocationMock();
  TilingTaskCreator tilingTaskCreator = new TilingTaskCreator();

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
    doNothing().when(tilingJobMailerMock).accept(any());
    when(jobAnnotationProcessorMock.accept(any(), any(), any(), any(), any()))
        .thenReturn(ZoneDetectionJobAnnotationProcessor.AnnotationJobIds.builder().build());
    new ObjectsDetectorMockResponse(objectsDetectorMock)
        .apply(MOCK_DETECTION_RESPONSE_CONFIDENCE, POOL, OBJECT_DETECTION_SUCCESS_RATE);
    when(tilesDownloaderMock.apply(any()))
        .thenAnswer(
            (i) ->
                Paths.get(this.getClass().getClassLoader().getResource("mockData/lyon").toURI())
                    .toFile());
    when(bucketComponentMock.upload(any(), any())).thenReturn(new FileHash(SHA256, "mock"));
  }

  @NonNull
  private static List<LocalEventQueue.CustomEventDelayConfig> customEventConfigList() {
    return List.of(
        new LocalEventQueue.CustomEventDelayConfig(AutoTaskStatisticRecomputingSubmitted.class, 50),
        new LocalEventQueue.CustomEventDelayConfig(
            ParcelDetectionStatusRecomputingSubmitted.class, 50),
        new LocalEventQueue.CustomEventDelayConfig(ZTJStatusRecomputingSubmitted.class, 50),
        new LocalEventQueue.CustomEventDelayConfig(ZDJStatusRecomputingSubmitted.class, 50));
  }

  @SneakyThrows
  @Test
  void ztj_created_and_ztj_and_zdj_succeed() {
    var tilingJob = zoneTilingJobId();

    eventProducerMock.accept(
        List.of(ZoneTilingJobCreated.builder().zoneTilingJob(tilingJob).build()));

    Thread.sleep(Duration.ofSeconds(90L));
    if (localEventQueue != null) localEventQueue.attemptSchedulerShutDown();

    var actualTilingJob = ztjRepository.findById(tilingJob.getId()).orElseThrow();
    var actualDetectionJob = zdjService.getByTilingJobId(tilingJob.getId(), MACHINE);
    assertTrue(actualTilingJob.isSucceeded());
    assertTrue(actualDetectionJob.isSucceeded());
  }

  @NonNull
  private ZoneTilingJob zoneTilingJobId() {
    var fullDetectionId = randomUUID().toString();
    var endToEndId = randomUUID().toString();
    var tilingJobId = randomUUID().toString();
    var tilingJob =
        ztjRepository.save(
            zoneTilingJobCreator.create(
                tilingJobId, "dummyZoneName", "dummy@email.com", PENDING, UNKNOWN));
    var parcel = parcelRepository.save(parcelCreator.create(1));
    var tilingTask =
        tilingTaskCreator.create(
            randomUUID().toString(), tilingJob.getId(), parcel, PENDING, UNKNOWN);
    tilingTaskRepository.save(tilingTask);
    fullDetectionRepository.save(
        FullDetection.builder()
            .id(fullDetectionId)
            .endToEndId(endToEndId)
            .ztjId(tilingJobId)
            .detectableObjectConfiguration(
                new DetectableObjectConfiguration()
                    .bucketStorageName(null)
                    .type(ROOF)
                    .confidence(new BigDecimal(1)))
            .build());
    return tilingJob;
  }
}
