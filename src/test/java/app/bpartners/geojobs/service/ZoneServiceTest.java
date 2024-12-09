package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.controller.DetectionControllerIT.defaultComputedStatistic;
import static app.bpartners.geojobs.endpoint.rest.model.DetectionStepName.*;
import static app.bpartners.geojobs.endpoint.rest.model.Status.HealthEnum.SUCCEEDED;
import static app.bpartners.geojobs.endpoint.rest.model.Status.HealthEnum.UNKNOWN;
import static app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role.ROLE_ADMIN;
import static app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role.ROLE_COMMUNITY;
import static app.bpartners.geojobs.file.hash.FileHashAlgorithm.SHA256;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.*;
import static app.bpartners.geojobs.repository.model.GeoJobType.DETECTION;
import static app.bpartners.geojobs.service.event.GeoJsonConversionTaskConsumer.GEO_JSON_BUCKET_FOLDER;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static java.io.File.createTempFile;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.DetectionSaved;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.DetectableObjectTypeMapper;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.DetectionStepStatisticMapper;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.StatusMapper;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.ZoneTilingJobMapper;
import app.bpartners.geojobs.endpoint.rest.model.CreateDetection;
import app.bpartners.geojobs.endpoint.rest.model.CreateZoneTilingJob;
import app.bpartners.geojobs.endpoint.rest.model.Detection;
import app.bpartners.geojobs.endpoint.rest.model.DetectionStep;
import app.bpartners.geojobs.endpoint.rest.model.GeoServerProperties;
import app.bpartners.geojobs.endpoint.rest.model.Status;
import app.bpartners.geojobs.endpoint.rest.security.AuthProvider;
import app.bpartners.geojobs.endpoint.rest.security.model.Authority;
import app.bpartners.geojobs.endpoint.rest.security.model.Principal;
import app.bpartners.geojobs.endpoint.rest.validator.FeatureMultiPolygonChecker;
import app.bpartners.geojobs.endpoint.rest.validator.ZoneDetectionJobValidator;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.file.hash.FileHash;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.statistic.TaskStatistic;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.ZoneDetectionJobRepository;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.GeoJobType;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.detection.DetectionGeoJsonUpdateValidator;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.service.geojson.GeoJsonConversionJobService;
import app.bpartners.geojobs.service.tiling.ZoneTilingJobService;
import app.bpartners.geojobs.utils.FeatureCreator;
import app.bpartners.geojobs.utils.detection.DetectionCreator;
import app.bpartners.geojobs.utils.detection.ZoneDetectionJobCreator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ZoneServiceTest {
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
  ZoneTilingJobService tilingJobServiceMock = mock();
  ZoneTilingJobMapper tilingJobMapperMock = mock();
  ZoneDetectionJobValidator detectionJobValidatorMock = mock();
  EventProducer eventProducerMock = mock();
  DetectionStepStatisticMapper stepStatisticMapper =
      new DetectionStepStatisticMapper(new StatusMapper<>());
  ZoneDetectionJobRepository zoneDetectionJobRepositoryMock = mock();
  DetectionRepository detectionRepositoryMock = mock();
  CommunityUsedSurfaceService communityUsedSurfaceServiceMock = mock();
  BucketComponent bucketComponentMock = mock();
  GeoJsonConversionJobService conversionInitiationServiceMock = mock();
  DetectableObjectTypeMapper detectableObjectTypeMapper = new DetectableObjectTypeMapper();
  ZoneDetectionJobService zoneDetectionJobServiceMock = mock();
  FeatureCreator featureCreator = new FeatureCreator();
  DetectionCreator detectionCreator = new DetectionCreator(featureCreator);
  AuthProvider authProviderMock = mock();
  DetectionGeoJsonUpdateValidator detectionGeoJsonUpdateValidator =
      new DetectionGeoJsonUpdateValidator();
  FeatureMultiPolygonChecker featureMultiPolygonCheckerMock = mock();
  ZoneDetectionJobCreator zoneDetectionJobCreator = new ZoneDetectionJobCreator();
  CommunityAuthorizationRepository communityAuthRepositoryMock = mock();
  ZoneService subject =
      new ZoneService(
          zoneDetectionJobServiceMock,
          tilingJobServiceMock,
          tilingJobMapperMock,
          detectionJobValidatorMock,
          eventProducerMock,
          stepStatisticMapper,
          detectionRepositoryMock,
          communityUsedSurfaceServiceMock,
          bucketComponentMock,
          conversionInitiationServiceMock,
          detectableObjectTypeMapper,
          new ObjectMapper().configure(FAIL_ON_UNKNOWN_PROPERTIES, false),
          authProviderMock,
          detectionGeoJsonUpdateValidator,
          featureMultiPolygonCheckerMock,
          communityAuthRepositoryMock);

  @BeforeEach
  void setUp() {
    when(communityAuthRepositoryMock.findByApiKey(any()))
        .thenReturn(
            Optional.of(CommunityAuthorization.builder().id(randomUUID().toString()).build()));
  }

  @Test
  void community_role_stuck_in_configuring() {
    when(authProviderMock.getPrincipal())
        .thenReturn(new Principal("mockApiKey", Set.of(new Authority(ROLE_COMMUNITY))));
    var detectionId = "mockDetectionId";
    String communityOwnerId = "communityOwnerId";
    var detection = detectionCreator.create(detectionId, null, null);
    var createDetection = new CreateDetection().geoJsonZone(featureCreator.defaultFeatures());
    when(detectionRepositoryMock.findByEndToEndIdAndCommunityOwnerId(detectionId, communityOwnerId))
        .thenReturn(Optional.of(detection));

    assertThrows(
        ApiException.class,
        () -> subject.processDetection(detectionId, createDetection, communityOwnerId),
        "A detection job with the specified id=(mockDetectionId) already exists and can not be"
            + " updated.");
  }

  @Test
  void admin_role_can_process_tiling() {
    var detectionId = randomUUID().toString();
    var detection = detectionCreator.create(detectionId, null, null);
    detection.setGeoServerProperties(new GeoServerProperties());
    detection.setMultiPolygonGeoJsonZone(List.of());
    var createDetection = new CreateDetection().geoJsonZone(featureCreator.defaultFeatures());
    String communityOwnerId = null;
    setUpAuthorityRoleProcessingMock(detectionId, detection, ROLE_ADMIN);

    var actual = subject.processDetection(detectionId, createDetection, communityOwnerId);

    assertEquals(TILING, actual.getStep().getName());
    assertEquals(Status.ProgressionEnum.PENDING, actual.getStep().getStatus().getProgression());
    assertEquals(UNKNOWN, actual.getStep().getStatus().getHealth());
  }

  @Test
  void stuck_at_configuring_when_multipolygon_geojson_or_geoserver_properties_are_null() {
    var detectionId = randomUUID().toString();
    var detection = detectionCreator.create(detectionId, null, null);
    var createDetection = new CreateDetection().geoJsonZone(featureCreator.defaultFeatures());
    String communityOwnerId = null;
    setUpAuthorityRoleProcessingMock(detectionId, detection, ROLE_ADMIN);

    var actual = subject.processDetection(detectionId, createDetection, communityOwnerId);

    assertEquals(CONFIGURING, actual.getStep().getName());
    assertEquals(Status.ProgressionEnum.PENDING, actual.getStep().getStatus().getProgression());
    assertEquals(UNKNOWN, actual.getStep().getStatus().getHealth());
  }

  @Test
  void read_detection_ko() {
    var detectionId = "NonExistentDetectionId";
    setUpAuthorityRoleProcessingMock(null, null, ROLE_ADMIN);

    assertThrows(
        ApiException.class,
        () -> subject.getProcessedDetection(detectionId),
        "DetectionJob.id=NonExistentDetectionId is not found.");
  }

  @Test
  void read_detection_ok() {
    var detectionId = randomUUID().toString();
    var tilingId = randomUUID().toString();
    var detection = detectionCreator.create(detectionId, tilingId, null);
    detection.setMultiPolygonGeoJsonZone(List.of());
    setUpAuthorityRoleProcessingMock(detectionId, detection, ROLE_ADMIN);
    var statistics = defaultComputedStatistic(detection.getId(), DETECTION);
    statistics.setActualJobStatus(
        JobStatus.builder()
            .progression(PENDING)
            .health(app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN)
            .build());
    when(zoneDetectionJobServiceMock.computeTaskStatistics(any())).thenReturn(statistics);
    when(tilingJobServiceMock.computeTaskStatistics(any())).thenReturn(statistics);

    var actual = subject.getProcessedDetection(detectionId);

    assertEquals(CONFIGURING, actual.getStep().getName());
    assertEquals(Status.ProgressionEnum.PENDING, actual.getStep().getStatus().getProgression());
    assertEquals(UNKNOWN, actual.getStep().getStatus().getHealth());
  }

  @SneakyThrows
  @Test
  void read_detection_with_geo_json_file() {
    var detectionId = randomUUID().toString();
    when(detectionRepositoryMock.findByEndToEndIdAndCommunityOwnerId(eq(detectionId), any()))
        .thenReturn(
            Optional.of(
                app.bpartners.geojobs.repository.model.detection.Detection.builder()
                    .geojsonS3FileKey("notNullKey")
                    .build()));
    when(bucketComponentMock.presign(any(), any())).thenReturn(new URI("http://localhost").toURL());
    var principalMock = mock(Principal.class);
    when(principalMock.getPassword()).thenReturn("dummy");
    when(authProviderMock.getPrincipal()).thenReturn(principalMock);

    var actual = subject.getProcessedDetection(detectionId);

    assertEquals(HUMAN_DETECTION, actual.getStep().getName());
    assertEquals(Status.ProgressionEnum.FINISHED, actual.getStep().getStatus().getProgression());
    assertEquals(SUCCEEDED, actual.getStep().getStatus().getHealth());
  }

  @Test
  void community_role_stuck_in_configuring_finished_reading_detection_ok() {
    var detectionId = randomUUID().toString();
    var tilingId = randomUUID().toString();
    var detection = detectionCreator.create(detectionId, tilingId, null);
    detection.setMultiPolygonGeoJsonZone(List.of(new Feature()));
    setUpAuthorityRoleProcessingMock(detectionId, detection, ROLE_COMMUNITY);

    var actual = subject.getProcessedDetection(detectionId);

    assertEquals(CONFIGURING, actual.getStep().getName());
    assertEquals(Status.ProgressionEnum.FINISHED, actual.getStep().getStatus().getProgression());
    assertEquals(SUCCEEDED, actual.getStep().getStatus().getHealth());
  }

  @Test
  void admin_role_read_detection_with_tiling_statistics() {
    var detectionId = randomUUID().toString();
    var tilingId = randomUUID().toString();
    var detection = detectionCreator.create(detectionId, tilingId, null);
    detection.setMultiPolygonGeoJsonZone(List.of(new Feature()));
    setUpAuthorityRoleProcessingMock(detectionId, detection, ROLE_ADMIN);

    var actual = subject.getProcessedDetection(detectionId);

    assertEquals(TILING, actual.getStep().getName());
    assertEquals(Status.ProgressionEnum.PENDING, actual.getStep().getStatus().getProgression());
    assertEquals(UNKNOWN, actual.getStep().getStatus().getHealth());
  }

  @Test
  void admin_role_read_detection_with_machine_detection_statistics() {
    var detectionId = randomUUID().toString();
    var tilingId = randomUUID().toString();
    var detectionJobId = randomUUID().toString();
    var detection = detectionCreator.create(detectionId, tilingId, detectionJobId);
    detection.setMultiPolygonGeoJsonZone(List.of(new Feature()));
    setUpAuthorityRoleProcessingMock(detectionId, detection, ROLE_ADMIN);
    when(zoneDetectionJobServiceMock.findById(detectionJobId))
        .thenReturn(
            zoneDetectionJobCreator.create(
                detectionJobId,
                null,
                null,
                PENDING,
                app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN,
                new ZoneTilingJob()));

    var actual = subject.getProcessedDetection(detectionId);

    assertEquals(MACHINE_DETECTION, actual.getStep().getName());
    assertEquals(Status.ProgressionEnum.PENDING, actual.getStep().getStatus().getProgression());
    assertEquals(UNKNOWN, actual.getStep().getStatus().getHealth());
  }

  @Test
  void admin_role_read_detection_with_human_detection_statistics() {
    var detectionId = randomUUID().toString();
    var tilingId = randomUUID().toString();
    var detectionJobId = randomUUID().toString();
    var detection = detectionCreator.create(detectionId, tilingId, detectionJobId);
    detection.setMultiPolygonGeoJsonZone(List.of(new Feature()));
    setUpAuthorityRoleProcessingMock(detectionId, detection, ROLE_ADMIN);
    when(zoneDetectionJobServiceMock.findById(detectionJobId))
        .thenReturn(
            zoneDetectionJobCreator.create(
                detectionJobId,
                null,
                null,
                FINISHED,
                app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED,
                new ZoneTilingJob()));

    var actual = subject.getProcessedDetection(detectionId);

    assertEquals(HUMAN_DETECTION, actual.getStep().getName());
    assertEquals(Status.ProgressionEnum.PROCESSING, actual.getStep().getStatus().getProgression());
    assertEquals(UNKNOWN, actual.getStep().getStatus().getHealth());
  }

  @SneakyThrows
  @Test
  void admin_role_read_detection_with_generated_geo_json() {
    var detectionId = randomUUID().toString();
    var tilingId = randomUUID().toString();
    var detectionJobId = randomUUID().toString();
    var detection = detectionCreator.create(detectionId, tilingId, detectionJobId);
    var geoJsonS3FileKey = "https://dummyGeoJsonFileKey.com";
    detection.setGeojsonS3FileKey(geoJsonS3FileKey);
    detection.setMultiPolygonGeoJsonZone(List.of(new Feature()));
    setUpAuthorityRoleProcessingMock(detectionId, detection, ROLE_ADMIN);
    when(zoneDetectionJobServiceMock.findById(detectionJobId))
        .thenReturn(
            zoneDetectionJobCreator.create(
                detectionJobId,
                null,
                null,
                FINISHED,
                app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED,
                new ZoneTilingJob()));
    when(bucketComponentMock.presign(any(), any())).thenReturn(new URI(geoJsonS3FileKey).toURL());

    var actual = subject.getProcessedDetection(detectionId);

    assertEquals(HUMAN_DETECTION, actual.getStep().getName());
    assertEquals(Status.ProgressionEnum.FINISHED, actual.getStep().getStatus().getProgression());
    assertEquals(SUCCEEDED, actual.getStep().getStatus().getHealth());
  }

  private void setUpAuthorityRoleProcessingMock(
      String detectionId,
      app.bpartners.geojobs.repository.model.detection.Detection detection,
      Authority.Role authorityRole) {
    if (detectionId != null && detection != null) {
      when(detectionRepositoryMock.findByEndToEndIdAndCommunityOwnerId(eq(detectionId), any()))
          .thenReturn(Optional.of(detection));
      when(detectionRepositoryMock.findById(detectionId)).thenReturn(Optional.of(detection));
    }
    when(authProviderMock.getPrincipal())
        .thenReturn(new Principal("mockApiKey", Set.of(new Authority(authorityRole))));
    when(tilingJobMapperMock.from(any()))
        .thenReturn(new CreateZoneTilingJob().geoServerUrl("http://localhost"));
    when(tilingJobMapperMock.toDomain(any())).thenReturn(new ZoneTilingJob());
    when(tilingJobServiceMock.create(any(), any())).thenReturn(new ZoneTilingJob());
    when(detectionRepositoryMock.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    when(tilingJobServiceMock.computeTaskStatistics(any()))
        .thenReturn(somePendingTaskStatistic(GeoJobType.TILING));
    when(zoneDetectionJobServiceMock.computeTaskStatistics(any()))
        .thenReturn(somePendingTaskStatistic(DETECTION));
  }

  private static TaskStatistic somePendingTaskStatistic(GeoJobType geoJobType) {
    return TaskStatistic.builder()
        .actualJobStatus(
            JobStatus.builder()
                .progression(PENDING)
                .health(app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN)
                .creationDatetime(now())
                .build())
        .updatedAt(now())
        .taskStatusStatistics(new ArrayList<>())
        .jobType(geoJobType)
        .build();
  }

  @SneakyThrows
  @Test
  void configure_shape_file_ok() {
    var shapeFile = createTempFile(randomUUID().toString(), randomUUID().toString());
    var detection =
        detectionCreator.create(
            randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), null);
    var detectionE2eId = detection.getEndToEndId();
    var shapeFileBucketKey = "detections/shape/" + detectionE2eId;
    var shapeUrl = "https://localhost";
    when(bucketComponentMock.upload(shapeFile, shapeFileBucketKey))
        .thenReturn(new FileHash(SHA256, "dummy"));
    when(bucketComponentMock.presign(any(), any())).thenReturn(new URI(shapeUrl).toURL());
    when(detectionRepositoryMock.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    when(detectionRepositoryMock.findByEndToEndIdAndCommunityOwnerId(eq(detectionE2eId), any()))
        .thenReturn(Optional.of(detection));
    var principalMock = mock(Principal.class);
    when(principalMock.getPassword()).thenReturn("dummy");
    when(authProviderMock.getPrincipal()).thenReturn(principalMock);

    var actual = subject.configureShapeFile(detectionE2eId, shapeFile);

    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock, only()).accept(listCaptor.capture());
    var detectionSaved = (DetectionSaved) listCaptor.getValue().getFirst();
    var expectedSavedDetection = detection.toBuilder().shapeFileKey(shapeFileBucketKey).build();
    var expectedDetectionSavedEvent =
        DetectionSaved.builder().detection(expectedSavedDetection).build();
    var expectedRestDetection =
        new Detection()
            .id(detectionE2eId)
            .shapeUrl(shapeUrl)
            .excelUrl(null)
            .geoJsonZone(null)
            .geoServerProperties(detection.getGeoServerProperties())
            .detectableObjectModel(detection.getDetectableObjectModel())
            .step(
                new DetectionStep()
                    .name(CONFIGURING)
                    .status(
                        new Status()
                            .progression(Status.ProgressionEnum.PENDING)
                            .health(UNKNOWN)
                            .creationDatetime(actual.getStep().getStatus().getCreationDatetime()))
                    .statistics(List.of())
                    .updatedAt(actual.getStep().getUpdatedAt()));
    assertEquals(expectedDetectionSavedEvent, detectionSaved);
    assertEquals(expectedRestDetection, actual);
  }

  @Test
  @SneakyThrows
  void configure_excel_file_ok() {
    var excelFile = createTempFile(randomUUID().toString(), randomUUID().toString());
    var detection =
        detectionCreator.create(
            randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), List.of());
    var detectionE2eId = detection.getEndToEndId();
    var excelFileBucketKey = "detections/excel/" + detectionE2eId;
    var excelUrl = "https://localhost";
    when(bucketComponentMock.upload(excelFile, excelFileBucketKey))
        .thenReturn(new FileHash(SHA256, "dummy"));
    when(bucketComponentMock.presign(any(), any())).thenReturn(new URI(excelUrl).toURL());
    when(detectionRepositoryMock.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    when(detectionRepositoryMock.findByEndToEndIdAndCommunityOwnerId(eq(detectionE2eId), any()))
        .thenReturn(Optional.of(detection));
    var principalMock = mock(Principal.class);
    when(principalMock.getPassword()).thenReturn("dummy");
    when(authProviderMock.getPrincipal()).thenReturn(principalMock);

    var actual = subject.configureExcelFile(detectionE2eId, excelFile);

    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock, only()).accept(listCaptor.capture());
    var detectionSaved = (DetectionSaved) listCaptor.getValue().getFirst();
    var expectedSavedDetection = detection.toBuilder().excelFileKey(excelFileBucketKey).build();
    var expectedDetectionSavedEvent =
        DetectionSaved.builder().detection(expectedSavedDetection).build();
    var expectedRestDetection =
        new Detection()
            .id(detectionE2eId)
            .excelUrl(excelUrl)
            .shapeUrl(null)
            .geoJsonZone(detection.getProvidedGeoJsonZone())
            .geoServerProperties(detection.getGeoServerProperties())
            .detectableObjectModel(detection.getDetectableObjectModel())
            .step(
                new DetectionStep()
                    .name(CONFIGURING)
                    .status(
                        new Status()
                            .progression(Status.ProgressionEnum.PENDING)
                            .health(UNKNOWN)
                            .creationDatetime(actual.getStep().getStatus().getCreationDatetime()))
                    .statistics(List.of())
                    .updatedAt(actual.getStep().getUpdatedAt()));
    assertEquals(expectedDetectionSavedEvent, detectionSaved);
    assertEquals(expectedRestDetection, actual);
  }

  @Test
  void finalize_geo_json_configuring_ko() {
    var featuresFile = new File(FEATURE_FILE_NAME_KO);
    var detection =
        detectionCreator.create(
            randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), null);
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
        detectionCreator.create(
            randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), List.of());
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
        detection.toBuilder()
            .providedGeoJsonZone(
                featureCreator.defaultFeatures().stream()
                    .map(FeatureMapper::toDomainFeature)
                    .toList())
            .build();
    var expectedRestDetection =
        new Detection()
            .id(detectionId)
            .geoJsonZone(featureCreator.defaultFeatures())
            .geoServerProperties(detection.getGeoServerProperties())
            .detectableObjectModel(detection.getDetectableObjectModel())
            .step(
                new DetectionStep()
                    .name(CONFIGURING)
                    .status(
                        new Status()
                            .progression(Status.ProgressionEnum.PROCESSING)
                            .health(UNKNOWN)
                            .creationDatetime(actual.getStep().getStatus().getCreationDatetime()))
                    .statistics(List.of())
                    .updatedAt(actual.getStep().getUpdatedAt()));
    assertEquals(
        DetectionSaved.builder().detection(expectedDetectionSaved).build(), detectionProvided);
    assertEquals(expectedDetectionSaved, savedDetection);
    assertEquals(expectedRestDetection, actual);
  }

  @Test
  void configure_geo_json_result() {
    var detectionId = randomUUID().toString();
    var fileMock = mock(File.class);
    when(detectionRepositoryMock.findById(detectionId))
        .thenReturn(Optional.of(new app.bpartners.geojobs.repository.model.detection.Detection()));
    when(detectionRepositoryMock.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));

    var actual = subject.configureGeoJsonResult(detectionId, fileMock);

    assertEquals(HUMAN_DETECTION, actual.getStep().getName());
    assertEquals(Status.ProgressionEnum.FINISHED, actual.getStep().getStatus().getProgression());
    assertEquals(SUCCEEDED, actual.getStep().getStatus().getHealth());
    var stringCaptor = ArgumentCaptor.forClass(String.class);
    verify(bucketComponentMock, only()).upload(eq(fileMock), stringCaptor.capture());
    verify(detectionRepositoryMock).save(any());
    assertTrue(stringCaptor.getValue().contains(GEO_JSON_BUCKET_FOLDER));
  }

  @SneakyThrows
  @Test
  void unable_to_update_geo_json() {
    var featuresFile = new File(FEATURE_FILE_NAME_OK);
    var shapeFile = createTempFile(randomUUID().toString(), randomUUID().toString());
    var excelFile = createTempFile(randomUUID().toString(), randomUUID().toString());
    var detection1 =
        detectionCreator.create(
            randomUUID().toString(),
            randomUUID().toString(),
            randomUUID().toString(),
            featureCreator.defaultFeatures());
    var detection2 =
        detectionCreator
            .create(
                randomUUID().toString(),
                randomUUID().toString(),
                randomUUID().toString(),
                List.of())
            .toBuilder()
            .shapeFileKey("notNullShapeFileKey")
            .build();
    var detection3 =
        detectionCreator
            .create(
                randomUUID().toString(),
                randomUUID().toString(),
                randomUUID().toString(),
                List.of())
            .toBuilder()
            .excelFileKey("notNullExcelFileKey")
            .build();
    when(detectionRepositoryMock.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    when(detectionRepositoryMock.findById(detection1.getId())).thenReturn(Optional.of(detection1));
    when(detectionRepositoryMock.findByEndToEndIdAndCommunityOwnerId(
            eq(detection2.getEndToEndId()), any()))
        .thenReturn(Optional.of(detection2));
    when(detectionRepositoryMock.findByEndToEndIdAndCommunityOwnerId(
            eq(detection3.getEndToEndId()), any()))
        .thenReturn(Optional.of(detection3));
    var principalMock = mock(Principal.class);
    when(principalMock.getPassword()).thenReturn("dummy");
    when(authProviderMock.getPrincipal()).thenReturn(principalMock);

    var actual1 =
        assertThrows(
            BadRequestException.class,
            () -> subject.finalizeGeoJsonConfig(detection1.getId(), featuresFile));
    var actual2 =
        assertThrows(
            BadRequestException.class,
            () -> subject.configureExcelFile(detection2.getEndToEndId(), excelFile));
    var actual3 =
        assertThrows(
            BadRequestException.class,
            () -> subject.configureShapeFile(detection3.getEndToEndId(), shapeFile));

    assertEquals(
        "Unable to finalize Detection(id="
            + detection1.getId()
            + ") geoJson as it already has values",
        actual1.getMessage());
    assertEquals(
        "Unable to configure Detection(id="
            + detection2.getId()
            + ") geoJson as it is already being configuring",
        actual2.getMessage());
    assertEquals(
        "Unable to configure Detection(id="
            + detection3.getId()
            + ") geoJson as it is already being configuring",
        actual3.getMessage());
  }
}
