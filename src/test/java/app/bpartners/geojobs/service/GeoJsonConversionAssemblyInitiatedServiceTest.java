package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PROCESSING;
import static app.bpartners.geojobs.repository.model.GeoJobType.GEO_JSON_CONVERSION;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.GeoJsonConversionAssemblyInitiated;
import app.bpartners.geojobs.endpoint.event.model.GeoJsonConversionAssemblySucceeded;
import app.bpartners.geojobs.file.ExtensionGuesser;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.file.hash.FileHash;
import app.bpartners.geojobs.file.hash.FileHashAlgorithm;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.job.model.TaskStatus;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.ClassPathResource;

class GeoJsonConversionAssemblyInitiatedServiceTest {
  private static final String GEO_JSON_CONVERSION_JOB_ID = "geoJsonConversionJobId";
  private static final String ZONE_DETECTION_JOB_ID = "zoneDetectionJobId";
  GeoJsonConversionTaskRepository geoJsonConversionTaskRepositoryMock = mock();
  GeoJsonConversionJobRepository geoJsonConversionJobRepositoryMock = mock();
  BucketComponent bucketComponentMock = mock();
  EventProducer eventProducerMock = mock();
  ZoneDetectionJobService zoneDetectionJobServiceMock = mock();
  DetectionRepository detectionRepositoryMock = mock();
  ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
  FileWriter fileWriter = new FileWriter(objectMapper, new ExtensionGuesser());
  GeoJsonConversionAssemblyInitiatedService subject =
      new GeoJsonConversionAssemblyInitiatedService(
          geoJsonConversionTaskRepositoryMock,
          geoJsonConversionJobRepositoryMock,
          bucketComponentMock,
          fileWriter,
          zoneDetectionJobServiceMock,
          detectionRepositoryMock,
          eventProducerMock,
          objectMapper);
  private final File featureFile;

  @SneakyThrows
  public GeoJsonConversionAssemblyInitiatedServiceTest() {
    this.featureFile = new ClassPathResource("features/features-ok.json").getFile();
  }

  @SneakyThrows
  @Test
  void upload_assembled_geo_json_and_update_detection_geo_json_file_key() {
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
        .thenReturn(dummyFinishedZDJ(ZONE_DETECTION_JOB_ID));
    when(bucketComponentMock.download(conversionTask1.getFileKey())).thenReturn(featureFile);
    when(bucketComponentMock.download(conversionTask2.getFileKey())).thenReturn(featureFile);
    when(bucketComponentMock.upload(any(), any()))
        .thenReturn(new FileHash(FileHashAlgorithm.SHA256, "dummy"));
    var detectionMock = new Detection();
    var optionalDetection = Optional.of(detectionMock);
    when(detectionRepositoryMock.findByZdjId(ZONE_DETECTION_JOB_ID)).thenReturn(optionalDetection);
    when(detectionRepositoryMock.save(any(Detection.class)))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    when(zoneDetectionJobServiceMock.getHumanZdjFromZdjId(any()))
        .thenReturn(dummyFinishedZDJ(ZONE_DETECTION_JOB_ID));
    when(zoneDetectionJobServiceMock.getMachineZdjFromZdjId(any())).thenReturn(null);

    assertDoesNotThrow(
        () ->
            subject.accept(
                GeoJsonConversionAssemblyInitiated.builder()
                    .geoJsonConversionJobId(GEO_JSON_CONVERSION_JOB_ID)
                    .build()));

    var fileKeyCaptor = ArgumentCaptor.forClass(String.class);
    var fileCaptor = ArgumentCaptor.forClass(File.class);
    verify(bucketComponentMock, times(1)).upload(fileCaptor.capture(), fileKeyCaptor.capture());
    var fileKey = fileKeyCaptor.getValue();
    var file = fileCaptor.getValue();
    var finalGeoJsonContent = Files.readString(Path.of(file.getPath()));
    assertEquals("dummyZoneName-final.geojson.txt", file.getName());
    assertEquals("geoJson/zoneDetectionJobId/dummyZoneName-final.geojson", fileKey);
    assertEquals(expectedFinalGeojsonContent(), finalGeoJsonContent);

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
    var geoJsonConversionAssemblySucceededEvent =
        (GeoJsonConversionAssemblySucceeded) eventCaptor.getValue().getFirst();
    assertEquals(
        GeoJsonConversionAssemblySucceeded.builder()
            .geoJsonConversionJob(savedGeoJsonConversionJob)
            .build(),
        geoJsonConversionAssemblySucceededEvent);
  }

  @SneakyThrows
  @Test
  void upload_assembled_geo_json_without_detection_geo_json_file_key_update() {
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
        .thenReturn(dummyProcessingZDJ(ZONE_DETECTION_JOB_ID));
    when(bucketComponentMock.download(conversionTask1.getFileKey())).thenReturn(featureFile);
    when(bucketComponentMock.download(conversionTask2.getFileKey())).thenReturn(featureFile);
    when(bucketComponentMock.upload(any(), any()))
        .thenReturn(new FileHash(FileHashAlgorithm.SHA256, "dummy"));
    var detectionMock = new Detection();
    var optionalDetection = Optional.of(detectionMock);
    when(detectionRepositoryMock.findByZdjId(ZONE_DETECTION_JOB_ID)).thenReturn(optionalDetection);
    when(detectionRepositoryMock.save(any(Detection.class)))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    when(zoneDetectionJobServiceMock.getHumanZdjFromZdjId(any()))
        .thenReturn(dummyProcessingZDJ(ZONE_DETECTION_JOB_ID));
    when(zoneDetectionJobServiceMock.getMachineZdjFromZdjId(any())).thenReturn(null);

    assertDoesNotThrow(
        () ->
            subject.accept(
                GeoJsonConversionAssemblyInitiated.builder()
                    .geoJsonConversionJobId(GEO_JSON_CONVERSION_JOB_ID)
                    .build()));

    var fileKeyCaptor = ArgumentCaptor.forClass(String.class);
    var fileCaptor = ArgumentCaptor.forClass(File.class);
    verify(bucketComponentMock, times(1)).upload(fileCaptor.capture(), fileKeyCaptor.capture());
    var fileKey = fileKeyCaptor.getValue();

    var geoJsonConversionJobCaptor = ArgumentCaptor.forClass(GeoJsonConversionJob.class);
    verify(geoJsonConversionJobRepositoryMock, times(1)).save(geoJsonConversionJobCaptor.capture());
    var savedGeoJsonConversionJob = geoJsonConversionJobCaptor.getValue();
    assertEquals(
        geoJsonConversionJob.toBuilder().fileKey(fileKey).build(), savedGeoJsonConversionJob);

    verify(detectionRepositoryMock, never()).findByZdjId(ZONE_DETECTION_JOB_ID);
    verify(detectionRepositoryMock, never()).save(any());
    verify(eventProducerMock, never()).accept(any());
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
        .thenReturn(dummyFinishedZDJ(zoneDetectionJobId));
    when(bucketComponentMock.download(conversionTask1.getFileKey())).thenReturn(featureFile);
    when(bucketComponentMock.download(conversionTask2.getFileKey())).thenReturn(featureFile);
    when(bucketComponentMock.upload(any(), any()))
        .thenReturn(new FileHash(FileHashAlgorithm.SHA256, "dummy"));
    when(detectionRepositoryMock.findByZdjId(any())).thenReturn(Optional.empty());
    when(detectionRepositoryMock.save(any(Detection.class)))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    when(zoneDetectionJobServiceMock.getHumanZdjFromZdjId(any()))
        .thenReturn(dummyFinishedZDJ(zoneDetectionJobId));
    when(zoneDetectionJobServiceMock.getMachineZdjFromZdjId(any()))
        .thenReturn(dummyFinishedZDJ(zoneDetectionJobId));

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

  private static ZoneDetectionJob dummyFinishedZDJ(String jobId) {
    return dummyZoneDetectionJob(jobId, FINISHED, SUCCEEDED);
  }

  private static ZoneDetectionJob dummyProcessingZDJ(String jobId) {
    return dummyZoneDetectionJob(jobId, PROCESSING, UNKNOWN);
  }

  private static ZoneDetectionJob dummyZoneDetectionJob(
      String jobId, Status.ProgressionStatus progressionStatus, Status.HealthStatus healthStatus) {
    return ZoneDetectionJob.builder()
        .id(jobId)
        .zoneName("dummyZoneName")
        .statusHistory(
            List.of(
                JobStatus.builder().progression(progressionStatus).health(healthStatus).build()))
        .build();
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

  private static @NotNull String expectedFinalGeojsonContent() {
    return """
{
  "features" : [ {
    "geometry" : {
      "coordinates" : [ [ [ [ 4.459648282829194, 45.904988912620688 ], [ 4.464709510872551, 45.928950368349426 ], [ 4.490816965688656, 45.941784543770964 ], [ 4.510354299995861, 45.933697132664598 ], [ 4.518386257467152, 45.912888345521047 ], [ 4.496344031095243, 45.883438201401809 ], [ 4.479593950305621, 45.882900828315755 ], [ 4.459648282829194, 45.904988912620688 ] ] ] ],
      "type" : "MultiPolygon"
    },
    "type" : "Feature",
    "properties" : {
      "code" : "69",
      "nom" : "Rhône",
      "id" : "feature1_id",
      "CLUSTER_ID" : 99520,
      "CLUSTER_SIZE" : 386884
    }
  }, {
    "geometry" : {
      "coordinates" : [ [ [ [ 4.459648282829194, 45.904988912620688 ], [ 4.464709510872551, 45.928950368349426 ], [ 4.490816965688656, 45.941784543770964 ], [ 4.510354299995861, 45.933697132664598 ], [ 4.518386257467152, 45.912888345521047 ], [ 4.496344031095243, 45.883438201401809 ], [ 4.479593950305621, 45.882900828315755 ], [ 4.459648282829194, 45.904988912620688 ] ] ] ],
      "type" : "MultiPolygon"
    },
    "type" : "Feature",
    "properties" : {
      "code" : "69",
      "nom" : "Rhône",
      "id" : "feature1_id",
      "CLUSTER_ID" : 99520,
      "CLUSTER_SIZE" : 386884
    }
  } ],
  "type" : "FeatureCollection"
}""";
  }
}
