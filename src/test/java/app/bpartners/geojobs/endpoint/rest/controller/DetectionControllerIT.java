package app.bpartners.geojobs.endpoint.rest.controller;

import static app.bpartners.geojobs.endpoint.rest.controller.mapper.StatusMapper.toHealthStatus;
import static app.bpartners.geojobs.endpoint.rest.controller.mapper.StatusMapper.toProgressionEnum;
import static app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role.ROLE_ADMIN;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PROCESSING;
import static app.bpartners.geojobs.repository.model.GeoJobType.DETECTION;
import static app.bpartners.geojobs.repository.model.GeoJobType.TILING;
import static app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.DetectionType.MACHINE;
import static app.bpartners.geojobs.service.event.ZoneDetectionFinishedConsumer.DEFAULT_MIN_CONFIDENCE;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.DetectionSaved;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.DetectionStepStatisticMapper;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.StatusMapper;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.ZoneDetectionJobMapper;
import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.endpoint.rest.security.AuthProvider;
import app.bpartners.geojobs.endpoint.rest.security.authorizer.DetectionAuthorizer;
import app.bpartners.geojobs.endpoint.rest.security.model.Authority;
import app.bpartners.geojobs.endpoint.rest.security.model.Principal;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.statistic.HealthStatusStatistic;
import app.bpartners.geojobs.job.model.statistic.TaskStatistic;
import app.bpartners.geojobs.job.model.statistic.TaskStatusStatistic;
import app.bpartners.geojobs.job.repository.JobStatusRepository;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.model.page.BoundedPageSize;
import app.bpartners.geojobs.model.page.PageFromOne;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.DetectableObjectConfigurationRepository;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.HumanDetectionJobRepository;
import app.bpartners.geojobs.repository.ParcelDetectionTaskRepository;
import app.bpartners.geojobs.repository.ParcelRepository;
import app.bpartners.geojobs.repository.ZoneDetectionJobRepository;
import app.bpartners.geojobs.repository.ZoneTilingJobRepository;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.ZoneService;
import app.bpartners.geojobs.service.annotator.AnnotationService;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.service.tiling.ZoneTilingJobService;
import app.bpartners.geojobs.utils.FeatureCreator;
import app.bpartners.geojobs.utils.detection.DetectionCreator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;

@Slf4j
class DetectionControllerIT extends FacadeIT {
  @Autowired ZoneDetectionController subject;
  @Autowired ZoneDetectionJobRepository jobRepository;
  @Autowired JobStatusRepository jobStatusRepository;
  @Autowired ParcelDetectionTaskRepository parcelDetectionTaskRepository;
  @Autowired ZoneDetectionJobMapper detectionJobMapper;
  @Autowired ParcelRepository parcelRepository;
  @Autowired ObjectMapper om;
  @Autowired DetectionRepository detectionRepository;
  @Autowired ZoneTilingJobRepository zoneTilingJobRepository;
  @Autowired ZoneDetectionJobRepository zoneDetectionJobRepository;
  @Autowired ZoneService zoneService;
  @Autowired DetectionStepStatisticMapper detectionStepStatisticMapper;
  @MockBean EventProducer eventProducer;
  @MockBean AnnotationService annotationServiceMock;
  @MockBean HumanDetectionJobRepository humanDetectionJobRepositoryMock;
  @MockBean BucketComponent bucketComponentMock;
  @MockBean ZoneDetectionJobService zoneDetectionJobService;
  @MockBean StatusMapper statusMapper;
  @MockBean ZoneTilingJobService zoneTilingJobService;
  @MockBean DetectableObjectConfigurationRepository detectableObjectConfigurationRepository;
  @MockBean DetectionAuthorizer detectionAuthorizer;
  @MockBean CommunityAuthorizationRepository communityAuthRepository;
  @MockBean AuthProvider authProviderMock;
  @Autowired FileWriter fileWriter;
  FeatureCreator featureCreator = new FeatureCreator();
  DetectionCreator detectionCreator = new DetectionCreator(featureCreator);

  @BeforeEach
  void setUp() {
    when(authProviderMock.getPrincipal())
        .thenReturn(new Principal("mockApiKey", Set.of(new Authority(ROLE_ADMIN))));
    doNothing().when(detectionAuthorizer).accept(any(), any(), any());
    detectionRepository.deleteAll();
  }

  private Detection detectionWithoutZdj(String tilingJobId) {
    var detectionId = randomUUID().toString();
    return Detection.builder()
        .id(detectionId)
        .endToEndId(detectionId)
        .ztjId(tilingJobId)
        .zdjId(null)
        .geojsonS3FileKey(null)
        .detectableObjectConfigurations(
            List.of(
                app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration
                    .builder()
                    .bucketStorageName(null)
                    .objectType(DetectableType.TOITURE_REVETEMENT)
                    .confidence(DEFAULT_MIN_CONFIDENCE)
                    .build()))
        .geoJsonZone(featureCreator.defaultFeatures())
        .build();
  }

  private CreateDetection createDetection() {
    var detectableObjectModel = new DetectableObjectModel(new BPToitureModel());
    return new CreateDetection()
        .emailReceiver("mock@hotmail.com")
        .zoneName("Lyon")
        .detectableObjectModel(detectableObjectModel)
        .geoServerProperties(
            new GeoServerProperties()
                .geoServerUrl("https://data.grandlyon.com/fr/geoserv/grandlyon/ows")
                .geoServerParameter(defaultGeoServerParameter()))
        .geoJsonZone(featureCreator.defaultFeatures());
  }

  @SneakyThrows
  private GeoServerParameter defaultGeoServerParameter() {
    return om.readValue(
        "{\n"
            + "    \"service\": \"WMS\",\n"
            + "    \"request\": \"GetMap\",\n"
            + "    \"layers\": \"grandlyon:ortho_2018\",\n"
            + "    \"styles\": \"\",\n"
            + "    \"format\": \"image/png\",\n"
            + "    \"transparent\": true,\n"
            + "    \"version\": \"1.3.0\",\n"
            + "    \"width\": 256,\n"
            + "    \"height\": 256,\n"
            + "    \"srs\": \"EPSG:3857\"\n"
            + "  }",
        GeoServerParameter.class);
  }

  private ZoneTilingJob zoneTilingJob(String id) {
    return ZoneTilingJob.builder()
        .id(id)
        .zoneName("Lyon")
        .submissionInstant(Instant.now())
        .emailReceiver("mock@hotmail.com")
        .statusHistory(
            List.of(
                JobStatus.builder()
                    .progression(FINISHED)
                    .health(SUCCEEDED)
                    .jobId(id)
                    .jobType(TILING)
                    .build()))
        .build();
  }

  private app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob zoneDetectionJob(
      String detectionJobId, String tilingJobId) {
    return app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.builder()
        .id(detectionJobId)
        .zoneTilingJob(zoneTilingJob(tilingJobId))
        .detectionType(MACHINE)
        .emailReceiver("mock@hotmail.com")
        .zoneName("Lyon")
        .submissionInstant(Instant.now())
        .statusHistory(
            List.of(
                JobStatus.builder()
                    .progression(PENDING)
                    .health(UNKNOWN)
                    .jobId(detectionJobId)
                    .jobType(DETECTION)
                    .build()))
        .build();
  }

  @Test
  void create_detection_without_tiling() {
    var savedTilingJob = zoneTilingJobRepository.save(zoneTilingJob(randomUUID().toString()));
    var savedDetectionJob =
        zoneDetectionJobRepository.save(
            zoneDetectionJob(randomUUID().toString(), savedTilingJob.getId()));
    var detection =
        detectionRepository.save(
            detectionCreator.createFromZTJAndZDJ(
                savedTilingJob.getId(), savedDetectionJob.getId()));
    when(zoneDetectionJobService.getByTilingJobId(any(), any())).thenReturn(savedDetectionJob);
    when(zoneDetectionJobService.processZDJ(any(), any())).thenReturn(savedDetectionJob);
    when(zoneDetectionJobService.computeTaskStatistics(any()))
        .thenReturn(defaultComputedStatistic(savedDetectionJob.getId(), DETECTION));
    when(statusMapper.toRest(any())).thenReturn(defaultSucceededStatus());

    subject.processDetection(detection.getId(), createDetection());

    verify(zoneDetectionJobService, times(1)).processZDJ(any(), any());
  }

  private Status defaultSucceededStatus() {
    return new Status()
        .progression(toProgressionEnum(FINISHED))
        .health(toHealthStatus(SUCCEEDED))
        .creationDatetime(now());
  }

  private TaskStatistic defaultComputedStatistic(
      String jobId, app.bpartners.geojobs.job.model.JobType jobType) {
    return TaskStatistic.builder()
        .jobType(jobType)
        .jobId(jobId)
        .taskStatusStatistics(
            List.of(
                TaskStatusStatistic.builder()
                    .progression(PROCESSING)
                    .healthStatusStatistics(
                        List.of(HealthStatusStatistic.builder().healthStatus(UNKNOWN).build()))
                    .taskStatistic(TaskStatistic.builder().jobId(jobId).jobType(jobType).build())
                    .build()))
        .build();
  }

  @Test
  void create_detection() {
    var detectionId = randomUUID().toString();
    var zoneTilingJob = zoneTilingJobRepository.save(zoneTilingJob(randomUUID().toString()));
    when(zoneTilingJobService.create(any(), any())).thenReturn(zoneTilingJob);
    when(zoneTilingJobService.computeTaskStatistics(any()))
        .thenReturn(defaultComputedStatistic(zoneTilingJob.getId(), TILING));

    subject.processDetection(detectionId, createDetection());

    verify(zoneTilingJobService, times(1)).create(any(), any());
  }

  @Test
  void create_detection_from_scratch() {
    var zoneTilingJob = zoneTilingJobRepository.save(zoneTilingJob(randomUUID().toString()));
    when(zoneTilingJobService.create(any(), any())).thenReturn(zoneTilingJob);
    when(zoneTilingJobService.computeTaskStatistics(any()))
        .thenReturn(defaultComputedStatistic(zoneTilingJob.getId(), TILING));

    subject.processDetection(randomUUID().toString(), createDetection());

    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(zoneTilingJobService, times(1)).create(any(), any());
    verify(eventProducer, only()).accept(listCaptor.capture());
    var detectionSavedEvent = (DetectionSaved) listCaptor.getValue().getFirst();
    assertEquals(
        DetectionSaved.builder().detection(detectionSavedEvent.getDetection()).build(),
        detectionSavedEvent);
    assertEquals(Duration.ofMinutes(3L), detectionSavedEvent.maxConsumerDuration());
    assertEquals(Duration.ofMinutes(1L), detectionSavedEvent.maxConsumerBackoffBetweenRetries());
  }

  @Test
  void get_detections_with_owner() {
    var zoneTilingJob = zoneTilingJobRepository.save(zoneTilingJob(randomUUID().toString()));
    var zoneDetectionJob =
        zoneDetectionJobRepository.save(
            zoneDetectionJob(randomUUID().toString(), zoneTilingJob.getId()));
    var detection =
        detectionRepository.save(
            detectionCreator.createFromZTJAndZDJ(zoneTilingJob.getId(), zoneDetectionJob.getId()));
    when(communityAuthRepository.findByApiKey(any())).thenReturn(Optional.of(mock()));
    var statistic = defaultComputedStatistic(zoneDetectionJob.getId(), DETECTION);
    when(zoneDetectionJobService.getTaskStatistic(any(String.class))).thenReturn(statistic);

    var actual = subject.getDetections(new PageFromOne(1), new BoundedPageSize(10));

    var expected =
        new app.bpartners.geojobs.endpoint.rest.model.Detection()
            .id(detection.getEndToEndId())
            .geoJsonZone(featureCreator.defaultFeatures())
            .actualStepStatus(
                detectionStepStatisticMapper.toRestDetectionStepStatus(
                    statistic, DetectionStep.MACHINE_DETECTION));
    assertEquals(List.of(expected), actual);
  }

  @Test
  void get_detections_without_owner_and_without_zdj() {
    var zoneTilingJob = zoneTilingJobRepository.save(zoneTilingJob(randomUUID().toString()));
    var detection = detectionRepository.save(detectionWithoutZdj(zoneTilingJob.getId()));
    when(communityAuthRepository.findByApiKey(any())).thenReturn(Optional.empty());
    var statistic = defaultComputedStatistic(zoneTilingJob.getId(), TILING);
    when(zoneTilingJobService.getTaskStatistic(any(String.class))).thenReturn(statistic);

    var actual = subject.getDetections(new PageFromOne(1), new BoundedPageSize(10));

    var expected =
        new app.bpartners.geojobs.endpoint.rest.model.Detection()
            .id(detection.getEndToEndId())
            .geoJsonZone(featureCreator.defaultFeatures())
            .actualStepStatus(
                detectionStepStatisticMapper.toRestDetectionStepStatus(
                    statistic, DetectionStep.TILING));
    assertEquals(List.of(expected), actual);
  }

  @SneakyThrows
  @Test
  void configure_file_shape_ok() {
    var presignedUrl = "http://localhost/presignedShapeUrl";
    File dummyShapeFile = new ClassPathResource("/shape/dummy.shape").getFile();
    var zoneTilingJob = zoneTilingJobRepository.save(zoneTilingJob(randomUUID().toString()));
    var detection = detectionRepository.save(detectionWithoutZdj(zoneTilingJob.getId()));
    when(bucketComponentMock.presign(any(), any())).thenReturn(new URI(presignedUrl).toURL());

    var actual =
        subject.configureDetectionShapeFile(
            detection.getId(), fileWriter.writeAsByte(dummyShapeFile));

    assertNull(detection.getShapeFileKey());
    assertEquals(presignedUrl, actual.getShapeUrl());
  }

  @SneakyThrows
  @Test
  void configure_file_excel_ko() {
    var dummyShapeFileBytes = new ClassPathResource("/shape/dummy.shape").getContentAsByteArray();
    var zoneTilingJob = zoneTilingJobRepository.save(zoneTilingJob(randomUUID().toString()));
    var detection = detectionRepository.save(detectionWithoutZdj(zoneTilingJob.getId()));

    var actual =
        assertThrows(
            BadRequestException.class,
            () -> subject.configureDetectionExcelFile(detection.getId(), dummyShapeFileBytes));

    var expected =
        "Only open office file (docx, xlsx, xls) media type is accepted but provided was :"
            + " text/plain";
    assertEquals(expected, actual.getMessage());
  }

  @SneakyThrows
  @Test
  void configure_file_modern_excel_ok() {
    var presignedUrl = "http://localhost/presignedExcelUrl";
    var modernExcelFile = new ClassPathResource("/excel/excelFile.xlsx").getContentAsByteArray();
    var zoneTilingJob = zoneTilingJobRepository.save(zoneTilingJob(randomUUID().toString()));
    var detection = detectionRepository.save(detectionWithoutZdj(zoneTilingJob.getId()));
    when(bucketComponentMock.presign(any(), any())).thenReturn(new URI(presignedUrl).toURL());

    var actual = subject.configureDetectionExcelFile(detection.getId(), modernExcelFile);

    assertNull(detection.getExcelFileKey());
    assertEquals(presignedUrl, actual.getExcelUrl());
  }
}
