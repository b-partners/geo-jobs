package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.model.SubscriptionConsumptionType.ROOF_ANALYSIS;
import static app.bpartners.geojobs.model.SubscriptionConsumptionUnit.UNIT;
import static app.bpartners.geojobs.repository.model.GeoJobType.GEO_JSON_CONVERSION;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.GeoJsonConversionAssemblyInitiated;
import app.bpartners.geojobs.endpoint.event.model.GeoJsonConversionAssemblySucceeded;
import app.bpartners.geojobs.endpoint.rest.security.AuthProvider;
import app.bpartners.geojobs.endpoint.rest.security.model.Principal;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.file.hash.FileHash;
import app.bpartners.geojobs.file.hash.FileHashAlgorithm;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.model.SubscriptionConsumptionLog;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.GeoJsonConversionJobRepository;
import app.bpartners.geojobs.repository.GeoJsonConversionTaskRepository;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.geojson.GeoJsonConversionJob;
import app.bpartners.geojobs.repository.model.geojson.GeoJsonConversionTask;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.service.event.GeoJsonConversionAssemblyInitiatedService;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GeoJsonConversionAssemblyInitiatedServiceTest {
  private static final String GEO_JSON_CONVERSION_JOB_ID = "geoJsonConversionJobId";
  public static final String ZONE_DETECTION_JOB_ID = "zoneDetectionJobId";
  GeoJsonConversionTaskRepository geoJsonConversionTaskRepositoryMock = mock();
  GeoJsonConversionJobRepository geoJsonConversionJobRepositoryMock = mock();
  BucketComponent bucketComponentMock = mock();
  FileWriter fileWriterMock = mock();
  EventProducer eventProducerMock = mock();
  ZoneDetectionJobService zoneDetectionJobServiceMock = mock();
  DetectionRepository detectionRepositoryMock = mock();
  AuthProvider authProviderMock = mock();
  SubscriptionConsumptionLogService subscriptionConsumptionLogServiceMock = mock();
  GeoJsonConversionAssemblyInitiatedService subject =
      new GeoJsonConversionAssemblyInitiatedService(
          geoJsonConversionTaskRepositoryMock,
          geoJsonConversionJobRepositoryMock,
          bucketComponentMock,
          fileWriterMock,
          zoneDetectionJobServiceMock,
          detectionRepositoryMock,
          subscriptionConsumptionLogServiceMock,
          eventProducerMock);

  @SneakyThrows
  @Test
  void accept_ok() {
    var geoJsonConversionJob =
        GeoJsonConversionJob.builder()
            .id(GEO_JSON_CONVERSION_JOB_ID)
            .zoneDetectionJobId(ZONE_DETECTION_JOB_ID)
            .build();
    var conversionTask1 =
        create(
            "conversionTask1",
            GEO_JSON_CONVERSION_JOB_ID,
            "conversionTaskFileKey1",
            1,
            FINISHED,
            SUCCEEDED);
    var conversionTask2 =
        create(
            "conversionTask2",
            GEO_JSON_CONVERSION_JOB_ID,
            "conversionTaskFileKey2",
            2,
            FINISHED,
            SUCCEEDED);
    when(geoJsonConversionJobRepositoryMock.findById(GEO_JSON_CONVERSION_JOB_ID))
        .thenReturn(Optional.of(geoJsonConversionJob));
    when(geoJsonConversionJobRepositoryMock.save(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(geoJsonConversionTaskRepositoryMock.findAllByJobId(GEO_JSON_CONVERSION_JOB_ID))
        .thenReturn(List.of(conversionTask1, conversionTask2));
    when(zoneDetectionJobServiceMock.findById(ZONE_DETECTION_JOB_ID))
        .thenReturn(dummyZoneDetectionJob(ZONE_DETECTION_JOB_ID));
    when(bucketComponentMock.download(conversionTask1.getFileKey())).thenReturn(mock(File.class));
    when(bucketComponentMock.download(conversionTask2.getFileKey())).thenReturn(mock(File.class));
    var combinedConvertedGeoJsonFileMock = mock(File.class);
    when(fileWriterMock.combineContent(any(), any())).thenReturn(combinedConvertedGeoJsonFileMock);
    when(bucketComponentMock.upload(any(), any()))
        .thenReturn(new FileHash(FileHashAlgorithm.SHA256, "dummy"));
    var detectionMock = new Detection();
    var optionalDetection = Optional.of(detectionMock);
    var principalMock = mock(Principal.class);
    when(detectionRepositoryMock.findByZdjId(ZONE_DETECTION_JOB_ID)).thenReturn(optionalDetection);
    when(detectionRepositoryMock.save(any(Detection.class)))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    when(zoneDetectionJobServiceMock.getHumanZdjFromZdjId(any()))
        .thenReturn(dummyZoneDetectionJob(ZONE_DETECTION_JOB_ID));
    when(zoneDetectionJobServiceMock.getMachineZdjFromZdjId(any())).thenReturn(null);
    when(authProviderMock.getPrincipal()).thenReturn(principalMock);

    assertDoesNotThrow(
        () ->
            subject.accept(
                GeoJsonConversionAssemblyInitiated.builder()
                    .geoJsonConversionJobId(GEO_JSON_CONVERSION_JOB_ID)
                    .build()));

    var outputFileNameCaptor = ArgumentCaptor.forClass(String.class);
    verify(fileWriterMock, times(1)).combineContent(any(), outputFileNameCaptor.capture());
    var outputFileName = outputFileNameCaptor.getValue();
    assertEquals("dummyZoneName-final.geojson", outputFileName);

    var fileKeyCaptor = ArgumentCaptor.forClass(String.class);
    var fileCaptor = ArgumentCaptor.forClass(File.class);
    verify(bucketComponentMock, times(1)).upload(fileCaptor.capture(), fileKeyCaptor.capture());
    var fileKey = fileKeyCaptor.getValue();
    var file = fileCaptor.getValue();
    assertEquals(combinedConvertedGeoJsonFileMock, file);
    assertEquals("geoJson/zoneDetectionJobId/dummyZoneName-final.geojson", fileKey);

    var geoJsonConversionJobCaptor = ArgumentCaptor.forClass(GeoJsonConversionJob.class);
    verify(geoJsonConversionJobRepositoryMock, times(1)).save(geoJsonConversionJobCaptor.capture());
    var savedGeoJsonConversionJob = geoJsonConversionJobCaptor.getValue();
    assertEquals(
        geoJsonConversionJob.toBuilder().fileKey(fileKey).build(), savedGeoJsonConversionJob);

    var detectionCaptor = ArgumentCaptor.forClass(Detection.class);
    verify(detectionRepositoryMock, times(1)).findByZdjId(ZONE_DETECTION_JOB_ID);
    verify(detectionRepositoryMock).save(detectionCaptor.capture());
    var savedDetection = detectionCaptor.getValue();
    assertEquals(fileKey, savedDetection.getGeojsonS3FileKey());

    var eventCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock, only()).accept(eventCaptor.capture());
    var geoJsonConversionAssemblySucceededEvent = (GeoJsonConversionAssemblySucceeded) eventCaptor.getValue().getFirst();
    assertEquals(GeoJsonConversionAssemblySucceeded.builder()
            .geoJsonConversionJob(savedGeoJsonConversionJob)
            .build(), geoJsonConversionAssemblySucceededEvent);

    var subscriptionConsumptionCaptor = ArgumentCaptor.forClass(SubscriptionConsumptionLog.class);
    var consumptionExpected =
        SubscriptionConsumptionLog.builder()
            .id(null)
            .consumptionType(ROOF_ANALYSIS)
            .consumptionUnit(UNIT)
            .usageMetric(1L)
            .creationDatetime(null)
            .build();

    verify(subscriptionConsumptionLogServiceMock, times(1))
        .sendSubscriptionConsumptionLog(any(), subscriptionConsumptionCaptor.capture());
    var actual = subscriptionConsumptionCaptor.getValue();
    actual.setId(null);
    actual.setCreationDatetime(null);

    assertEquals(consumptionExpected, actual);
  }

  @SneakyThrows
  @Test
  void throw_exception_when_any_zdj_associated_to_detection() {
    var geoJsonConversionJobId = randomUUID().toString();
    var zoneDetectionJobId = randomUUID().toString();
    var geoJsonConversionJob =
        GeoJsonConversionJob.builder()
            .id(geoJsonConversionJobId)
            .zoneDetectionJobId(zoneDetectionJobId)
            .build();
    var conversionTask1 =
        create(
            "conversionTask1",
            geoJsonConversionJobId,
            "conversionTaskFileKey1",
            1,
            FINISHED,
            SUCCEEDED);
    var conversionTask2 =
        create(
            "conversionTask2",
            geoJsonConversionJobId,
            "conversionTaskFileKey2",
            2,
            FINISHED,
            SUCCEEDED);
    when(geoJsonConversionJobRepositoryMock.findById(geoJsonConversionJobId))
        .thenReturn(Optional.of(geoJsonConversionJob));
    when(geoJsonConversionJobRepositoryMock.save(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(geoJsonConversionTaskRepositoryMock.findAllByJobId(geoJsonConversionJobId))
        .thenReturn(List.of(conversionTask1, conversionTask2));
    when(zoneDetectionJobServiceMock.findById(zoneDetectionJobId))
        .thenReturn(dummyZoneDetectionJob(zoneDetectionJobId));
    when(bucketComponentMock.download(conversionTask1.getFileKey())).thenReturn(mock(File.class));
    when(bucketComponentMock.download(conversionTask2.getFileKey())).thenReturn(mock(File.class));
    var combinedConvertedGeoJsonFileMock = mock(File.class);
    when(fileWriterMock.combineContent(any(), any())).thenReturn(combinedConvertedGeoJsonFileMock);
    when(bucketComponentMock.upload(any(), any()))
        .thenReturn(new FileHash(FileHashAlgorithm.SHA256, "dummy"));
    when(detectionRepositoryMock.findByZdjId(any())).thenReturn(Optional.empty());
    when(detectionRepositoryMock.save(any(Detection.class)))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    when(zoneDetectionJobServiceMock.getHumanZdjFromZdjId(any()))
        .thenReturn(dummyZoneDetectionJob(zoneDetectionJobId));
    when(zoneDetectionJobServiceMock.getMachineZdjFromZdjId(any()))
        .thenReturn(dummyZoneDetectionJob(zoneDetectionJobId));

    var actual =
        assertThrows(
            NotFoundException.class,
            () ->
                subject.accept(
                    GeoJsonConversionAssemblyInitiated.builder()
                        .geoJsonConversionJobId(geoJsonConversionJobId)
                        .build()));

    assertEquals(
        "Any detection found associated to ZDJ(id=" + zoneDetectionJobId + ")",
        actual.getMessage());
  }

  private static ZoneDetectionJob dummyZoneDetectionJob(String jobId) {
    return ZoneDetectionJob.builder().id(jobId).zoneName("dummyZoneName").build();
  }

  GeoJsonConversionTask create(
      String taskId,
      String jobId,
      String fileKey,
      Integer page,
      Status.ProgressionStatus progressionStatus,
      Status.HealthStatus healthStatus) {
    var geoJsonConversionTask =
        GeoJsonConversionTask.builder()
            .id(taskId)
            .jobId(jobId)
            .fileKey(fileKey)
            .page(page)
            .statusHistory(new ArrayList<>())
            .build();
    geoJsonConversionTask.hasNewStatus(
        TaskStatus.builder()
            .id(randomUUID().toString())
            .creationDatetime(now())
            .jobType(GEO_JSON_CONVERSION)
            .taskId(taskId)
            .progression(progressionStatus)
            .health(healthStatus)
            .build());
    return geoJsonConversionTask;
  }
}
