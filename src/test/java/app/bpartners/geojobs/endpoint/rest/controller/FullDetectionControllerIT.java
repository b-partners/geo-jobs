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
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.StatusMapper;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.TaskStatisticMapper;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.ZoneDetectionJobMapper;
import app.bpartners.geojobs.endpoint.rest.model.CreateFullDetection;
import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectConfiguration;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.GeoServerParameter;
import app.bpartners.geojobs.endpoint.rest.model.JobType;
import app.bpartners.geojobs.endpoint.rest.model.Status;
import app.bpartners.geojobs.endpoint.rest.security.authorizer.CommunityFullDetectionAuthorizer;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.statistic.HealthStatusStatistic;
import app.bpartners.geojobs.job.model.statistic.TaskStatistic;
import app.bpartners.geojobs.job.model.statistic.TaskStatusStatistic;
import app.bpartners.geojobs.job.repository.JobStatusRepository;
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
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

@Slf4j
public class FullDetectionControllerIT extends FacadeIT {
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
  @MockBean CommunityFullDetectionAuthorizer communityFullDetectionAuthorizer;

  @BeforeEach
  void setUp() {
    doNothing().when(communityFullDetectionAuthorizer).accept(any());
  }

  private CreateFullDetection createFullDetection() throws JsonProcessingException {
    return new CreateFullDetection()
        .endToEndId("end_to_end_id")
        .objectType(ROOF)
        .confidence(BigDecimal.valueOf(0.8))
        .emailReceiver("mock@hotmail.com")
        .zoneName("Lyon")
        .geoServerUrl("https://data.grandlyon.com/fr/geoserv/grandlyon/ows")
        .zoomLevel(CreateFullDetection.ZoomLevelEnum.HOUSES_0)
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
                GeoServerParameter.class))
        .features(
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

  private ZoneTilingJob zoneTilingJob() {
    return ZoneTilingJob.builder()
        .id("ztj_1_id")
        .zoneName("Lyon")
        .submissionInstant(Instant.now())
        .emailReceiver("mock@hotmail.com")
        .statusHistory(
            List.of(
                JobStatus.builder()
                    .progression(FINISHED)
                    .health(SUCCEEDED)
                    .jobId("ztj_1_id")
                    .jobType(TILING)
                    .build()))
        .build();
  }

  private app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob zoneDetectionJob() {
    return app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.builder()
        .id("zdj_1_id")
        .zoneTilingJob(zoneTilingJob())
        .detectionType(MACHINE)
        .emailReceiver("mock@hotmail.com")
        .zoneName("Lyon")
        .submissionInstant(Instant.now())
        .statusHistory(
            List.of(
                JobStatus.builder()
                    .progression(PENDING)
                    .health(UNKNOWN)
                    .jobId("zdj_1_id")
                    .jobType(DETECTION)
                    .build()))
        .build();
  }

  private FullDetection fullDetection() {
    return FullDetection.builder()
        .endToEndId("end_to_end_id")
        .id("full_detection_id")
        .ztjId("ztj_1_id")
        .zdjId("zdj_1_id")
        .geojsonS3FileKey(null)
        .detectableObjectConfiguration(
            new DetectableObjectConfiguration().confidence(BigDecimal.valueOf(0.8)).type(ROOF))
        .build();
  }

  @Test
  void create_full_detection_without_tiling() throws JsonProcessingException {
    zoneTilingJobRepository.save(zoneTilingJob());
    zoneDetectionJobRepository.save(zoneDetectionJob());
    fullDetectionRepository.save(fullDetection());
    when(zoneDetectionJobService.processZDJ(any(), any())).thenReturn(zoneDetectionJob());
    when(zoneDetectionJobService.computeTaskStatistics(any()))
        .thenReturn(
            TaskStatistic.builder()
                .jobType(DETECTION)
                .jobId(zoneDetectionJob().getId())
                .taskStatusStatistics(
                    List.of(
                        TaskStatusStatistic.builder()
                            .progression(PROCESSING)
                            .healthStatusStatistics(
                                List.of(
                                    HealthStatusStatistic.builder().healthStatus(UNKNOWN).build()))
                            .taskStatistic(
                                TaskStatistic.builder()
                                    .jobId(zoneDetectionJob().getId())
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

    subject.processFullDetection(createFullDetection());

    verify(zoneDetectionJobService, times(1)).processZDJ(any(), any());
  }

  private FullDetection fullDetectionWithoutZTJAndZDJ() {
    return FullDetection.builder().id("full_detection_id").endToEndId("end_to_end_id").build();
  }

  @Test
  void create_full_detection() throws JsonProcessingException {
    fullDetectionRepository.save(fullDetectionWithoutZTJAndZDJ());
    when(zoneTilingJobService.create(any(), any(), any())).thenReturn(zoneTilingJob());
    when(zoneTilingJobService.computeTaskStatistics(any()))
        .thenReturn(TaskStatistic.builder().build());
    when(taskStatisticMapper.toRest(any()))
        .thenReturn(
            new app.bpartners.geojobs.endpoint.rest.model.TaskStatistic().jobType(JobType.TILING));

    subject.processFullDetection(createFullDetection());

    verify(zoneTilingJobService, times(1)).create(any(), any(), any());
  }
}
