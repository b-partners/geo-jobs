package app.bpartners.geojobs.endpoint.rest.controller;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.*;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.*;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.ZDJParcelsStatusRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.ZDJStatusRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.*;
import app.bpartners.geojobs.endpoint.rest.model.SuccessStatus;
import app.bpartners.geojobs.endpoint.rest.model.ZoneDetectionJob;
import app.bpartners.geojobs.endpoint.rest.security.authorizer.CommunityFullDetectionAuthorizer;
import app.bpartners.geojobs.endpoint.rest.validator.ZoneDetectionJobValidator;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.job.model.statistic.HealthStatusStatistic;
import app.bpartners.geojobs.job.model.statistic.TaskStatistic;
import app.bpartners.geojobs.job.model.statistic.TaskStatusStatistic;
import app.bpartners.geojobs.repository.DetectableObjectConfigurationRepository;
import app.bpartners.geojobs.repository.model.FilteredDetectionJob;
import app.bpartners.geojobs.repository.model.GeoJobType;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.ParcelService;
import app.bpartners.geojobs.service.ZoneService;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.service.geojson.GeoJsonConversionInitiationService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ZoneDetectionControllerTest {
  StatusMapper<JobStatus> statusMapper = new StatusMapper<>();
  ParcelService parcelServiceMock = mock();
  DetectableObjectConfigurationRepository objectConfigurationRepositoryMock = mock();
  ZoneDetectionJobService detectionJobServiceMock = mock();
  ZoneDetectionTypeMapper zoneDetectionTypeMapper = new ZoneDetectionTypeMapper();
  ZoneDetectionJobMapper detectionJobMapper =
      new ZoneDetectionJobMapper(statusMapper, zoneDetectionTypeMapper);
  DetectableObjectConfigurationMapper objectConfigurationMapper =
      new DetectableObjectConfigurationMapper(new DetectableObjectTypeMapper());
  DetectionTaskMapper taskMapper = new DetectionTaskMapper(mock());
  ZoneDetectionJobValidator jobValidator = new ZoneDetectionJobValidator(mock());
  TaskStatisticMapper taskStatisticMapper = new TaskStatisticMapper(statusMapper);
  EventProducer eventProducerMock = mock();
  CommunityFullDetectionAuthorizer communityFullDetectionAuthorizer = mock();
  GeoJsonConversionInitiationService geoJsonConversionInitiationServiceMock = mock();
  ZoneService zoneServiceMock = mock();
  ZoneDetectionController subject =
      new ZoneDetectionController(
          parcelServiceMock,
          objectConfigurationRepositoryMock,
          detectionJobServiceMock,
          detectionJobMapper,
          objectConfigurationMapper,
          taskMapper,
          jobValidator,
          taskStatisticMapper,
          statusMapper,
          eventProducerMock,
          geoJsonConversionInitiationServiceMock,
          communityFullDetectionAuthorizer,
          zoneServiceMock);

  @Test
  void task_filtering_ok() {
    String jobId = "jobId";
    var succeededJob = aZDJ("succeededJob", FINISHED, SUCCEEDED);
    var notSucceededJob = aZDJ("notSucceededJob", PENDING, UNKNOWN);
    when(detectionJobServiceMock.dispatchTasksBySucceededStatus(jobId))
        .thenReturn(new FilteredDetectionJob(jobId, succeededJob, notSucceededJob));

    var actual = subject.filteredDetectionJobs(jobId);

    var restSucceededJob =
        new app.bpartners.geojobs.endpoint.rest.model.FilteredDetectionJob()
            .status(SuccessStatus.SUCCEEDED)
            .job(detectionJobMapper.toRest(succeededJob, List.of()));
    var restNotSucceededJob =
        new app.bpartners.geojobs.endpoint.rest.model.FilteredDetectionJob()
            .status(SuccessStatus.NOT_SUCCEEDED)
            .job(detectionJobMapper.toRest(notSucceededJob, List.of()));
    assertEquals(List.of(restSucceededJob, restNotSucceededJob), actual);
  }

  @Test
  void get_zdj_recomputed_status_ok() {
    String jobId = "jobId";
    when(detectionJobServiceMock.findById(jobId))
        .thenReturn(
            aZDJ(
                jobId,
                app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING,
                app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN));

    var actual = subject.getZDJRecomputedStatus(jobId);

    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock, times(1)).accept(listCaptor.capture());
    var ztjStatusComputingEvent =
        ((List<ZDJStatusRecomputingSubmitted>) listCaptor.getValue()).getFirst();
    assertEquals(new ZDJStatusRecomputingSubmitted(jobId), ztjStatusComputingEvent);
    assertEquals(
        new app.bpartners.geojobs.endpoint.rest.model.Status()
            .progression(app.bpartners.geojobs.endpoint.rest.model.Status.ProgressionEnum.PENDING)
            .health(app.bpartners.geojobs.endpoint.rest.model.Status.HealthEnum.UNKNOWN)
            .creationDatetime(actual.getCreationDatetime()),
        actual);
  }

  @Test
  void get_zdj_tasks_recomputed_status_ok() {
    String jobId = "jobId";
    when(detectionJobServiceMock.findById(jobId))
        .thenReturn(
            aZDJ(
                jobId,
                app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING,
                app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN));

    var actual = subject.getZDJTasksRecomputedStatus(jobId);

    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock, times(1)).accept(listCaptor.capture());
    var ztjStatusComputingEvent =
        ((List<ZDJParcelsStatusRecomputingSubmitted>) listCaptor.getValue()).getFirst();
    assertEquals(new ZDJParcelsStatusRecomputingSubmitted(jobId), ztjStatusComputingEvent);
    assertEquals(
        new app.bpartners.geojobs.endpoint.rest.model.Status()
            .progression(app.bpartners.geojobs.endpoint.rest.model.Status.ProgressionEnum.PENDING)
            .health(app.bpartners.geojobs.endpoint.rest.model.Status.HealthEnum.UNKNOWN)
            .creationDatetime(actual.getCreationDatetime()),
        actual);
  }

  @Test
  void get_detection_task_statistics_ok() {
    String jobId = "jobId";
    var domainStatistic = aTaskStatistic(jobId);
    var expected = taskStatisticMapper.toRest(domainStatistic);
    when(detectionJobServiceMock.computeTaskStatistics(jobId)).thenReturn(domainStatistic);

    var actual = subject.getDetectionTaskStatistics(jobId);

    assertEquals(expected, actual);
  }

  @Test
  void process_failed_detection_job() {
    String jobId = "jobId";
    var failedJob = aZDJ(jobId, FINISHED, FAILED);
    ZoneDetectionJob expected = detectionJobMapper.toRest(failedJob, List.of());
    when(detectionJobServiceMock.retryFailedTask(jobId)).thenReturn(failedJob);

    var actual = subject.processFailedDetectionJob(jobId);

    assertEquals(expected, actual);
  }

  private static app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob aZDJ(
      String jobId, Status.ProgressionStatus progressionStatus, Status.HealthStatus healthStatus) {
    return app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.builder()
        .id(jobId)
        .zoneName("dummy")
        .emailReceiver("dummy")
        .statusHistory(
            List.of(
                JobStatus.builder()
                    .id(randomUUID().toString())
                    .jobId(jobId)
                    .progression(progressionStatus)
                    .health(healthStatus)
                    .creationDatetime(now())
                    .build()))
        .zoneTilingJob(new ZoneTilingJob())
        .build();
  }

  private static TaskStatistic aTaskStatistic(String jobId) {
    var statusStatistics =
        List.of(
            aStatusStatistic(PENDING), aStatusStatistic(PROCESSING), aStatusStatistic(FINISHED));
    var taskStatistic =
        TaskStatistic.builder()
            .id("taskStatisticId")
            .jobId(jobId)
            .actualJobStatus(
                JobStatus.builder()
                    .progression(PENDING)
                    .health(UNKNOWN)
                    .creationDatetime(now())
                    .build())
            .updatedAt(now())
            .jobType(GeoJobType.DETECTION)
            .build();
    taskStatistic.addStatusStatistics(statusStatistics);
    return taskStatistic;
  }

  private static TaskStatusStatistic aStatusStatistic(Status.ProgressionStatus progressionStatus) {
    var healthStatusStatistics =
        List.of(
            aHealthStatusStatistic(UNKNOWN),
            aHealthStatusStatistic(RETRYING),
            aHealthStatusStatistic(FAILED),
            aHealthStatusStatistic(SUCCEEDED));
    var taskStatusStatistic = TaskStatusStatistic.builder().progression(progressionStatus).build();
    taskStatusStatistic.addHealthStatusStatistics(healthStatusStatistics);
    return taskStatusStatistic;
  }

  private static HealthStatusStatistic aHealthStatusStatistic(Status.HealthStatus healthStatus) {
    return HealthStatusStatistic.builder().healthStatus(healthStatus).count(1L).build();
  }
}
