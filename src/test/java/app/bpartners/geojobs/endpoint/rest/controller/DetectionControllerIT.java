package app.bpartners.geojobs.endpoint.rest.controller;

import static app.bpartners.geojobs.endpoint.rest.controller.mapper.StatusMapper.toHealthStatus;
import static app.bpartners.geojobs.endpoint.rest.controller.mapper.StatusMapper.toProgressionEnum;
import static app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType.ROOF;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PROCESSING;
import static app.bpartners.geojobs.repository.model.GeoJobType.DETECTION;
import static app.bpartners.geojobs.repository.model.GeoJobType.TILING;
import static app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.DetectionType.MACHINE;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.FullDetectionSaved;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.StatusMapper;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.TaskStatisticMapper;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.ZoneDetectionJobMapper;
import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.endpoint.rest.security.AuthProvider;
import app.bpartners.geojobs.endpoint.rest.security.authorizer.DetectionAuthorizer;
import app.bpartners.geojobs.endpoint.rest.security.model.Principal;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.statistic.HealthStatusStatistic;
import app.bpartners.geojobs.job.model.statistic.TaskStatistic;
import app.bpartners.geojobs.job.model.statistic.TaskStatusStatistic;
import app.bpartners.geojobs.job.repository.JobStatusRepository;
import app.bpartners.geojobs.model.page.BoundedPageSize;
import app.bpartners.geojobs.model.page.PageFromOne;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.DetectableObjectConfigurationRepository;
import app.bpartners.geojobs.repository.FullDetectionRepository;
import app.bpartners.geojobs.repository.HumanDetectionJobRepository;
import app.bpartners.geojobs.repository.ParcelDetectionTaskRepository;
import app.bpartners.geojobs.repository.ParcelRepository;
import app.bpartners.geojobs.repository.ZoneDetectionJobRepository;
import app.bpartners.geojobs.repository.ZoneTilingJobRepository;
import app.bpartners.geojobs.repository.model.detection.FullDetection;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.ZoneService;
import app.bpartners.geojobs.service.annotator.AnnotationService;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.service.tiling.ZoneTilingJobService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

@Slf4j
@Disabled("TODO: fail")
class DetectionControllerIT extends FacadeIT {
  @Autowired ZoneDetectionController subject;
  @Autowired ZoneDetectionJobRepository jobRepository;
  @Autowired JobStatusRepository jobStatusRepository;
  @Autowired ParcelDetectionTaskRepository parcelDetectionTaskRepository;
  @Autowired ZoneDetectionJobMapper detectionJobMapper;
  @Autowired ParcelRepository parcelRepository;
  @MockBean EventProducer eventProducer;
  @MockBean AnnotationService annotationServiceMock;
  @MockBean HumanDetectionJobRepository humanDetectionJobRepositoryMock;
  @Autowired ObjectMapper om;
  @Autowired FullDetectionRepository fullDetectionRepository;
  @Autowired ZoneTilingJobRepository zoneTilingJobRepository;
  @Autowired ZoneDetectionJobRepository zoneDetectionJobRepository;
  @MockBean BucketComponent bucketComponent;
  @Autowired ZoneService zoneService;
  @MockBean ZoneDetectionJobService zoneDetectionJobService;
  @MockBean StatusMapper statusMapper;
  @MockBean ZoneTilingJobService zoneTilingJobService;
  @MockBean TaskStatisticMapper taskStatisticMapper;
  @MockBean DetectableObjectConfigurationRepository detectableObjectConfigurationRepository;
  @MockBean DetectionAuthorizer detectionAuthorizer;
  @MockBean CommunityAuthorizationRepository communityAuthRepository;

  @MockBean AuthProvider authProviderMock;

  @BeforeEach
  void setUp() {
    when(authProviderMock.getPrincipal()).thenReturn(mock(Principal.class));
    doNothing().when(detectionAuthorizer).accept(any(), any(), any());
    fullDetectionRepository.deleteAll();
  }

  private FullDetection fullDetectionWithoutZdj(String tilingJobId) {
    return FullDetection.builder()
        .endToEndId(randomUUID().toString())
        .id(randomUUID().toString())
        .ztjId(tilingJobId)
        .zdjId(null)
        .geojsonS3FileKey(null)
        .detectableObjectConfiguration(
            new DetectableObjectConfiguration().confidence(BigDecimal.valueOf(0.8)).type(ROOF))
        .build();
  }

  private CreateDetection createDetection() throws JsonProcessingException {
    return new CreateDetection()
        // .objectType(ROOF)
        // .confidence(BigDecimal.valueOf(0.8))
        .overallConfiguration(
            new DetectionOverallConfiguration()
                .emailReceiver("mock@hotmail.com")
                .zoneName("Lyon")
                .geoServerUrl("https://data.grandlyon.com/fr/geoserv/grandlyon/ows")
                .geoServerParameter(
                    om.readValue(
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
                        GeoServerParameter.class)))
        .geoJsonZone(
            List.of(
                om.readValue(
                        "{ \"type\": \"Feature\",\n"
                            + "  \"properties\": {\n"
                            + "    \"code\": \"69\",\n"
                            + "    \"nom\": \"Rh\u00f4ne\",\n"
                            + "    \"id\": 30251921,\n"
                            + "    \"CLUSTER_ID\": 99520,\n"
                            + "    \"CLUSTER_SIZE\": 386884 },\n"
                            + "  \"geometry\": {\n"
                            + "    \"type\": \"MultiPolygon\",\n"
                            + "    \"coordinates\": [ [ [\n"
                            + "      [ 4.459648282829194, 45.904988912620688 ],\n"
                            + "      [ 4.464709510872551, 45.928950368349426 ],\n"
                            + "      [ 4.490816965688656, 45.941784543770964 ],\n"
                            + "      [ 4.510354299995861, 45.933697132664598 ],\n"
                            + "      [ 4.518386257467152, 45.912888345521047 ],\n"
                            + "      [ 4.496344031095243, 45.883438201401809 ],\n"
                            + "      [ 4.479593950305621, 45.882900828315755 ],\n"
                            + "      [ 4.459648282829194, 45.904988912620688 ] ] ] ] } }",
                        Feature.class)
                    .id("feature_1_id")));
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

  private FullDetection fullDetection(String tilingJobId, String detectionJobId) {
    return FullDetection.builder()
        .endToEndId(randomUUID().toString())
        .id(randomUUID().toString())
        .ztjId(tilingJobId)
        .zdjId(detectionJobId)
        .geojsonS3FileKey(null)
        .detectableObjectConfiguration(
            new DetectableObjectConfiguration().confidence(BigDecimal.valueOf(0.8)).type(ROOF))
        .build();
  }

  @Test
  void create_full_detection_without_tiling() throws JsonProcessingException {
    var savedTilingJob = zoneTilingJobRepository.save(zoneTilingJob(randomUUID().toString()));
    var savedDetectionJob =
        zoneDetectionJobRepository.save(
            zoneDetectionJob(randomUUID().toString(), savedTilingJob.getId()));
    var fullDetection =
        fullDetectionRepository.save(
            fullDetection(savedTilingJob.getId(), savedDetectionJob.getId()));
    when(zoneDetectionJobService.getByTilingJobId(any(), any())).thenReturn(savedDetectionJob);
    when(zoneDetectionJobService.processZDJ(any(), any())).thenReturn(savedDetectionJob);
    when(zoneDetectionJobService.computeTaskStatistics(any()))
        .thenReturn(
            TaskStatistic.builder()
                .jobType(DETECTION)
                .jobId(savedDetectionJob.getId())
                .taskStatusStatistics(
                    List.of(
                        TaskStatusStatistic.builder()
                            .progression(PROCESSING)
                            .healthStatusStatistics(
                                List.of(
                                    HealthStatusStatistic.builder().healthStatus(UNKNOWN).build()))
                            .taskStatistic(
                                TaskStatistic.builder()
                                    .jobId(savedDetectionJob.getId())
                                    .jobType(DETECTION)
                                    .build())
                            .build()))
                .build());
    when(statusMapper.toRest(any()))
        .thenReturn(
            new Status()
                .progression(toProgressionEnum(FINISHED))
                .health(toHealthStatus(SUCCEEDED))
                .creationDatetime(now()));

    subject.processDetection(fullDetection.getId(), createDetection());

    verify(zoneDetectionJobService, times(1)).processZDJ(any(), any());
  }

  private FullDetection fullDetectionWithoutZTJAndZDJ() {
    return FullDetection.builder()
        .id(randomUUID().toString())
        .endToEndId(randomUUID().toString())
        .build();
  }

  @Test
  void create_full_detection() throws JsonProcessingException {
    var zoneTilingJob = zoneTilingJobRepository.save(zoneTilingJob(randomUUID().toString()));
    var fullDetection = fullDetectionRepository.save(fullDetectionWithoutZTJAndZDJ());
    when(zoneTilingJobService.create(any(), any())).thenReturn(zoneTilingJob);
    when(zoneTilingJobService.computeTaskStatistics(any()))
        .thenReturn(TaskStatistic.builder().build());
    when(taskStatisticMapper.toRest(any()))
        .thenReturn(
            new app.bpartners.geojobs.endpoint.rest.model.TaskStatistic().jobType(JobType.TILING));

    subject.processDetection(fullDetection.getEndToEndId(), createDetection());

    verify(zoneTilingJobService, times(1)).create(any(), any());
  }

  @Test
  void create_full_detection_from_scratch() throws JsonProcessingException {
    var zoneTilingJob = zoneTilingJobRepository.save(zoneTilingJob(randomUUID().toString()));
    when(zoneTilingJobService.create(any(), any())).thenReturn(zoneTilingJob);
    when(zoneTilingJobService.computeTaskStatistics(any()))
        .thenReturn(TaskStatistic.builder().build());
    when(taskStatisticMapper.toRest(any()))
        .thenReturn(
            new app.bpartners.geojobs.endpoint.rest.model.TaskStatistic().jobType(JobType.TILING));

    subject.processDetection(randomUUID().toString(), createDetection());

    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(zoneTilingJobService, times(1)).create(any(), any());
    verify(eventProducer, only()).accept(listCaptor.capture());
    var fullDetectionSavedEvent = (FullDetectionSaved) listCaptor.getValue().getFirst();
    assertEquals(
        FullDetectionSaved.builder()
            .fullDetection(fullDetectionSavedEvent.getFullDetection())
            .build(),
        fullDetectionSavedEvent);
    assertEquals(Duration.ofMinutes(3L), fullDetectionSavedEvent.maxConsumerDuration());
    assertEquals(
        Duration.ofMinutes(1L), fullDetectionSavedEvent.maxConsumerBackoffBetweenRetries());
  }

  @Test
  void get_full_detections_with_owner() {
    var zoneTilingJob = zoneTilingJobRepository.save(zoneTilingJob(randomUUID().toString()));
    var zoneDetectionJob =
        zoneDetectionJobRepository.save(
            zoneDetectionJob(randomUUID().toString(), zoneTilingJob.getId()));
    var fullDetection =
        fullDetectionRepository.save(
            fullDetection(zoneTilingJob.getId(), zoneDetectionJob.getId()));
    var statistics =
        new app.bpartners.geojobs.endpoint.rest.model.TaskStatistic().jobType(JobType.DETECTION);

    when(communityAuthRepository.findByApiKey(any())).thenReturn(Optional.of(mock()));
    when(zoneDetectionJobService.getTaskStatistic(any(String.class)))
        .thenReturn(TaskStatistic.builder().build());
    when(taskStatisticMapper.toRest(any())).thenReturn(statistics);

    var actual = subject.getFullDetections(new PageFromOne(1), new BoundedPageSize(10));
    var expected =
        new Detection()
            .id(fullDetection.getEndToEndId())
            .step(
                new DetectionStepStatus().step(DetectionStep.MACHINE_DETECTION)
                // .statistics(List.of()) TODO statistics
                );

    assertEquals(List.of(expected), actual);
  }

  @Test
  void get_full_detections_without_owner_and_without_zdj() {
    var zoneTilingJob = zoneTilingJobRepository.save(zoneTilingJob(randomUUID().toString()));
    var fullDetection =
        fullDetectionRepository.save(fullDetectionWithoutZdj(zoneTilingJob.getId()));
    var statistics =
        new app.bpartners.geojobs.endpoint.rest.model.TaskStatistic().jobType(JobType.TILING);

    when(communityAuthRepository.findByApiKey(any())).thenReturn(Optional.empty());
    when(zoneTilingJobService.getTaskStatistic(any(String.class)))
        .thenReturn(TaskStatistic.builder().build());
    when(taskStatisticMapper.toRest(any())).thenReturn(statistics);

    var actual = subject.getFullDetections(new PageFromOne(1), new BoundedPageSize(10));
    var expected =
        new Detection()
            .id(fullDetection.getEndToEndId())
            .step(
                new DetectionStepStatus().step(DetectionStep.TILING)
                // .statistics(List.of()) TODO statistics
                );

    assertEquals(List.of(expected), actual);
  }
}
