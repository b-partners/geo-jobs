package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.model.DetectionStep.CONFIGURING;
import static app.bpartners.geojobs.endpoint.rest.model.DetectionStep.TILING;
import static app.bpartners.geojobs.endpoint.rest.model.Status.HealthEnum.SUCCEEDED;
import static app.bpartners.geojobs.endpoint.rest.model.Status.HealthEnum.UNKNOWN;
import static app.bpartners.geojobs.endpoint.rest.model.Status.ProgressionEnum.*;
import static app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role.ROLE_ADMIN;
import static app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role.ROLE_COMMUNITY;
import static app.bpartners.geojobs.file.hash.FileHashAlgorithm.SHA256;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.DetectionSaved;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.DetectableObjectTypeMapper;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.DetectionStepStatisticMapper;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.StatusMapper;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.ZoneTilingJobMapper;
import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.endpoint.rest.security.AuthProvider;
import app.bpartners.geojobs.endpoint.rest.security.model.Authority;
import app.bpartners.geojobs.endpoint.rest.security.model.Principal;
import app.bpartners.geojobs.endpoint.rest.validator.ZoneDetectionJobValidator;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.file.hash.FileHash;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.statistic.TaskStatistic;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.ZoneDetectionJobRepository;
import app.bpartners.geojobs.repository.ZoneTilingJobRepository;
import app.bpartners.geojobs.repository.model.GeoJobType;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.service.geojson.GeoJsonConversionInitiationService;
import app.bpartners.geojobs.service.tiling.ZoneTilingJobService;
import app.bpartners.geojobs.utils.FeatureCreator;
import app.bpartners.geojobs.utils.detection.DetectionCreator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.net.URI;
import java.util.*;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ZoneServiceTest {
  ZoneTilingJobService tilingJobServiceMock = mock();
  ZoneTilingJobMapper tilingJobMapperMock = mock();
  ZoneDetectionJobValidator detectionJobValidatorMock = mock();
  EventProducer eventProducerMock = mock();
  DetectionStepStatisticMapper stepStatisticMapper =
      new DetectionStepStatisticMapper(new StatusMapper<>());
  ZoneDetectionJobRepository zoneDetectionJobRepositoryMock = mock();
  DetectionRepository detectionRepositoryMock = mock();
  ZoneTilingJobRepository tilingJobRepositoryMock = mock();
  CommunityUsedSurfaceService communityUsedSurfaceServiceMock = mock();
  BucketComponent bucketComponentMock = mock();
  GeoJsonConversionInitiationService conversionInitiationServiceMock = mock();
  DetectableObjectTypeMapper detectableObjectTypeMapper = new DetectableObjectTypeMapper();
  ZoneDetectionJobService zoneDetectionJobServiceMock = mock();
  DetectionUpdateValidator detectionUpdateValidatorMock = mock();
  FeatureCreator featureCreator = new FeatureCreator();
  DetectionCreator detectionCreator = new DetectionCreator(featureCreator);
  private static final String FEATURE_FILE_NAME_OK =
      "src"
          + File.separator
          + "test"
          + File.separator
          + "resources"
          + File.separator
          + "features"
          + File.separator
          + "features-ok.json";
  private static final String FEATURE_FILE_NAME_KO =
      "src"
          + File.separator
          + "test"
          + File.separator
          + "resources"
          + File.separator
          + "features"
          + File.separator
          + "features-ko.json";
  AuthProvider authProviderMock = mock();
  ZoneService subject =
      new ZoneService(
          zoneDetectionJobServiceMock,
          tilingJobServiceMock,
          tilingJobMapperMock,
          detectionJobValidatorMock,
          eventProducerMock,
          stepStatisticMapper,
          zoneDetectionJobRepositoryMock,
          detectionRepositoryMock,
          tilingJobRepositoryMock,
          communityUsedSurfaceServiceMock,
          bucketComponentMock,
          conversionInitiationServiceMock,
          detectableObjectTypeMapper,
          new ObjectMapper().configure(FAIL_ON_UNKNOWN_PROPERTIES, false),
          detectionUpdateValidatorMock,
          authProviderMock);

  @Test
  void community_role_stuck_in_configuring() {
    when(authProviderMock.getPrincipal())
        .thenReturn(new Principal("mockApiKey", Set.of(new Authority(ROLE_COMMUNITY))));
    var detectionId = randomUUID().toString();
    var detection = detectionCreator.create(detectionId, null, null);
    var createDetection = new CreateDetection().geoJsonZone(featureCreator.defaultFeatures());
    when(detectionRepositoryMock.findByEndToEndId(detectionId)).thenReturn(Optional.of(detection));
    Optional<String> communityOwnerId = Optional.empty();

    var actual = subject.processDetection(detectionId, createDetection, communityOwnerId);

    assertEquals(CONFIGURING, actual.getActualStepStatus().getStep());
    assertEquals(FINISHED, actual.getActualStepStatus().getStatus().getProgression());
    assertEquals(SUCCEEDED, actual.getActualStepStatus().getStatus().getHealth());
  }

  @Test
  void admin_role_can_process_tiling() {
    var detectionId = randomUUID().toString();
    var detection = detectionCreator.create(detectionId, null, null);
    var createDetection = new CreateDetection().geoJsonZone(featureCreator.defaultFeatures());
    Optional<String> communityOwnerId = Optional.empty();
    setUpAdminRoleCanProcessTilingMock(detectionId, detection);

    var actual = subject.processDetection(detectionId, createDetection, communityOwnerId);

    assertEquals(TILING, actual.getActualStepStatus().getStep());
    assertEquals(
        Status.ProgressionEnum.PENDING, actual.getActualStepStatus().getStatus().getProgression());
    assertEquals(UNKNOWN, actual.getActualStepStatus().getStatus().getHealth());
  }

  private void setUpAdminRoleCanProcessTilingMock(
      String detectionId, app.bpartners.geojobs.repository.model.detection.Detection detection) {
    when(detectionRepositoryMock.findByEndToEndId(detectionId)).thenReturn(Optional.of(detection));
    when(authProviderMock.getPrincipal())
        .thenReturn(new Principal("mockApiKey", Set.of(new Authority(ROLE_ADMIN))));
    when(tilingJobMapperMock.from(any()))
        .thenReturn(new CreateZoneTilingJob().geoServerUrl("http://localhost"));
    when(tilingJobMapperMock.toDomain(any())).thenReturn(new ZoneTilingJob());
    when(tilingJobServiceMock.create(any(), any())).thenReturn(new ZoneTilingJob());
    when(detectionRepositoryMock.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    when(tilingJobServiceMock.computeTaskStatistics(any()))
        .thenReturn(
            TaskStatistic.builder()
                .actualJobStatus(
                    JobStatus.builder()
                        .progression(PENDING)
                        .health(app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN)
                        .creationDatetime(now())
                        .build())
                .updatedAt(now())
                .taskStatusStatistics(new ArrayList<>())
                .jobType(GeoJobType.TILING)
                .build());
  }

  @SneakyThrows
  @Test
  void configure_shape_file_ok() {
    var shapeFile = File.createTempFile(randomUUID().toString(), randomUUID().toString());
    var detection =
        detectionCreator.createFromZTJAndZDJ(randomUUID().toString(), randomUUID().toString());
    var detectionId = detection.getId();
    var shapeFileBucketKey = "detections/shape/" + detectionId;
    var shapeUrl = "https://localhost";
    when(bucketComponentMock.upload(shapeFile, shapeFileBucketKey))
        .thenReturn(new FileHash(SHA256, "dummy"));
    when(bucketComponentMock.presign(any(), any())).thenReturn(new URI(shapeUrl).toURL());
    when(detectionRepositoryMock.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    when(detectionRepositoryMock.findById(detectionId)).thenReturn(Optional.of(detection));

    var actual = subject.configureShapeFile(detectionId, shapeFile);

    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock, only()).accept(listCaptor.capture());
    var detectionSaved = (DetectionSaved) listCaptor.getValue().getFirst();
    var expectedSavedDetection = detection.toBuilder().shapeFileKey(shapeFileBucketKey).build();
    var expectedDetectionSavedEvent =
        DetectionSaved.builder().detection(expectedSavedDetection).build();
    var expectedRestDetection =
        new Detection()
            .id(detectionId)
            .shapeUrl(shapeUrl)
            .excelUrl(null)
            .geoJsonZone(detection.getGeoJsonZone())
            .geoServerProperties(detection.getGeoServerProperties())
            .detectableObjectModel(detection.getDetectableObjectModel())
            .actualStepStatus(
                new DetectionStepStatus()
                    .step(CONFIGURING)
                    .status(
                        new Status()
                            .progression(PROCESSING)
                            .health(UNKNOWN)
                            .creationDatetime(
                                actual.getActualStepStatus().getStatus().getCreationDatetime()))
                    .statistics(List.of())
                    .updatedAt(actual.getActualStepStatus().getUpdatedAt()));
    assertEquals(expectedDetectionSavedEvent, detectionSaved);
    assertEquals(expectedRestDetection, actual);
  }

  @Test
  @SneakyThrows
  void configure_excel_file_ok() {
    var excelFile = File.createTempFile(randomUUID().toString(), randomUUID().toString());
    var detection =
        detectionCreator.createFromZTJAndZDJ(randomUUID().toString(), randomUUID().toString());
    var detectionId = detection.getId();
    var excelFileBucketKey = "detections/excel/" + detectionId;
    var excelUrl = "https://localhost";
    when(bucketComponentMock.upload(excelFile, excelFileBucketKey))
        .thenReturn(new FileHash(SHA256, "dummy"));
    when(bucketComponentMock.presign(any(), any())).thenReturn(new URI(excelUrl).toURL());
    when(detectionRepositoryMock.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    when(detectionRepositoryMock.findById(detectionId)).thenReturn(Optional.of(detection));

    var actual = subject.configureExcelFile(detectionId, excelFile);

    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock, only()).accept(listCaptor.capture());
    var detectionSaved = (DetectionSaved) listCaptor.getValue().getFirst();
    var expectedSavedDetection = detection.toBuilder().excelFileKey(excelFileBucketKey).build();
    var expectedDetectionSavedEvent =
        DetectionSaved.builder().detection(expectedSavedDetection).build();
    var expectedRestDetection =
        new Detection()
            .id(detectionId)
            .excelUrl(excelUrl)
            .shapeUrl(null)
            .geoJsonZone(detection.getGeoJsonZone())
            .geoServerProperties(detection.getGeoServerProperties())
            .detectableObjectModel(detection.getDetectableObjectModel())
            .actualStepStatus(
                new DetectionStepStatus()
                    .step(CONFIGURING)
                    .status(
                        new Status()
                            .progression(PROCESSING)
                            .health(UNKNOWN)
                            .creationDatetime(
                                actual.getActualStepStatus().getStatus().getCreationDatetime()))
                    .statistics(List.of())
                    .updatedAt(actual.getActualStepStatus().getUpdatedAt()));
    assertEquals(expectedDetectionSavedEvent, detectionSaved);
    assertEquals(expectedRestDetection, actual);
  }

  @Test
  void finalize_geo_json_configuring_ko() {
    var featuresFile = new File(FEATURE_FILE_NAME_KO);
    var detection =
        detectionCreator.createFromZTJAndZDJ(randomUUID().toString(), randomUUID().toString());
    var detectionId = detection.getId();
    when(detectionRepositoryMock.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    when(detectionRepositoryMock.findById(detectionId)).thenReturn(Optional.of(detection));

    var actual =
        assertThrows(
            ApiException.class, () -> subject.finalizeGeoJsonConfig(detectionId, featuresFile));

    assertTrue(
        actual.getMessage().contains("Unable to convert uploaded file to Features, exception="));
  }

  @SneakyThrows
  @Test
  void finalize_geo_json_configuring_ok() {
    var featuresFile = new File(FEATURE_FILE_NAME_OK);
    var detection =
        detectionCreator.createFromZTJAndZDJ(randomUUID().toString(), randomUUID().toString());
    var detectionId = detection.getId();
    when(detectionRepositoryMock.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    when(detectionRepositoryMock.findById(detectionId)).thenReturn(Optional.of(detection));

    var actual = subject.finalizeGeoJsonConfig(detectionId, featuresFile);

    var detectionCaptor =
        ArgumentCaptor.forClass(app.bpartners.geojobs.repository.model.detection.Detection.class);
    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(detectionRepositoryMock).save(detectionCaptor.capture());
    verify(eventProducerMock, only()).accept(listCaptor.capture());
    var savedDetection = detectionCaptor.getValue();
    var detectionProvided = (DetectionSaved) listCaptor.getValue().getFirst();
    var expectedDetectionSaved =
        detection.toBuilder().geoJsonZone(detection.getGeoJsonZone()).build();
    var expectedRestDetection =
        new Detection()
            .id(detectionId)
            .geoJsonZone(detection.getGeoJsonZone())
            .geoServerProperties(detection.getGeoServerProperties())
            .detectableObjectModel(detection.getDetectableObjectModel())
            .actualStepStatus(
                new DetectionStepStatus()
                    .step(CONFIGURING)
                    .status(
                        new Status()
                            .progression(FINISHED)
                            .health(SUCCEEDED)
                            .creationDatetime(
                                actual.getActualStepStatus().getStatus().getCreationDatetime()))
                    .statistics(List.of())
                    .updatedAt(actual.getActualStepStatus().getUpdatedAt()));
    assertEquals(
        DetectionSaved.builder().detection(expectedDetectionSaved).build(), detectionProvided);
    assertEquals(expectedDetectionSaved, savedDetection);
    assertEquals(expectedRestDetection, actual);
  }
}
