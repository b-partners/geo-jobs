package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.model.BucketSeparatorType.SLASH;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.*;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.*;
import static app.bpartners.geojobs.repository.model.GeoJobType.DETECTION;
import static app.bpartners.geojobs.repository.model.GeoJobType.TILING;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.ImportedZoneTilingJobSaved;
import app.bpartners.geojobs.endpoint.event.model.TaskStatisticRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.ZoneTilingJobWithoutTasksCreated;
import app.bpartners.geojobs.endpoint.rest.model.BucketSeparatorType;
import app.bpartners.geojobs.endpoint.rest.model.GeoServerParameter;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.job.model.statistic.HealthStatusStatistic;
import app.bpartners.geojobs.job.model.statistic.TaskStatistic;
import app.bpartners.geojobs.job.repository.JobStatusRepository;
import app.bpartners.geojobs.job.repository.TaskRepository;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.model.FilteredTilingJob;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.tiling.TilingTask;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.service.tiling.ZoneTilingJobService;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.stubbing.Answer;
import org.springframework.data.jpa.repository.JpaRepository;

@Slf4j
public class ZoneTilingJobServiceTest {
  public static final String JOB_ID = "jobId";
  public static final String JOB2_ID = "job2Id";
  public static final String JOB_3_ID = "job3_id";
  public static final String JOB_4_ID = "job4Id";
  public static final String JOB_5_ID = "job5Id";
  public static final String JOB_ID_NOT_FOUND = "job_id_not_found";
  public static final String TO_DUPLICATE_JOB = "toDuplicateJob";
  JpaRepository<ZoneTilingJob, String> jobRepositoryMock = mock();
  JobStatusRepository jobStatusRepositoryMock = mock();
  TaskRepository<TilingTask> taskRepositoryMock = mock();
  EventProducer eventProducerMock = mock();
  ZoneDetectionJobService detectionJobServiceMock = mock();
  NotFinishedTaskRetriever<TilingTask> notFinishedTaskRetriever = new NotFinishedTaskRetriever<>();
  ZoneTilingJobService subject =
      new ZoneTilingJobService(
          jobRepositoryMock,
          jobStatusRepositoryMock,
          taskRepositoryMock,
          eventProducerMock,
          detectionJobServiceMock,
          mock(),
          notFinishedTaskRetriever);

  @Test
  void duplicate_ok() {
    ZoneTilingJob expectedJob =
        ZoneTilingJob.builder()
            .id(TO_DUPLICATE_JOB)
            .zoneName("DummyZoneName")
            .emailReceiver("DummyEmail")
            .submissionInstant(now())
            .statusHistory(
                List.of(
                    JobStatus.builder()
                        .progression(FINISHED)
                        .health(SUCCEEDED)
                        .creationDatetime(now())
                        .build()))
            .build();
    when(jobRepositoryMock.findById(TO_DUPLICATE_JOB)).thenReturn(Optional.of(expectedJob));

    ZoneTilingJob actual = subject.duplicate(TO_DUPLICATE_JOB);

    var eventCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock, times(1)).accept(eventCaptor.capture());
    List<ZoneTilingJobWithoutTasksCreated> events =
        (List<ZoneTilingJobWithoutTasksCreated>) eventCaptor.getValue();
    var zoneTilingJobWithoutTaskCreated = events.getFirst();
    assertEquals(expectedJob.duplicate(actual.getId()), actual);
    assertNotEquals(expectedJob.getId(), actual.getId());
    assertEquals(actual.getId(), zoneTilingJobWithoutTaskCreated.getDuplicatedJobId());
    assertEquals(expectedJob, zoneTilingJobWithoutTaskCreated.getOriginalJob());
  }

  @Test
  void import_from_bucket_ok() {
    when(jobRepositoryMock.save(ArgumentMatchers.any()))
        .thenAnswer(
            (Answer<ZoneTilingJob>)
                invocationOnMock -> {
                  Object[] args = invocationOnMock.getArguments();
                  return (ZoneTilingJob) args[0];
                });
    String importedJobId = "importedJobId";
    String bucketName = "bucketName";
    String bucketPathPrefix = "bucketPathPrefix";
    String geoServerUrlDummy = "geoServerUrlDummy";
    BucketSeparatorType bucketSeparatorType = SLASH;
    Long startFrom = 0L;
    Long endAt = 1L;
    GeoServerParameter geoServerParameter = new GeoServerParameter();
    ZoneTilingJob job =
        ZoneTilingJob.builder()
            .id(importedJobId)
            .zoneName("dummyZoneName")
            .emailReceiver("dummyEmailReceiver")
            .statusHistory(
                List.of(
                    JobStatus.builder()
                        .health(UNKNOWN)
                        .progression(PENDING)
                        .creationDatetime(now())
                        .jobId(importedJobId)
                        .build()))
            .build();
    ZoneTilingJob actual =
        subject.importFromBucket(
            job,
            bucketName,
            bucketPathPrefix,
            geoServerParameter,
            geoServerUrlDummy,
            startFrom,
            endAt,
            bucketSeparatorType);

    var eventCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock, times(1)).accept(eventCaptor.capture());
    List<ImportedZoneTilingJobSaved> events =
        (List<ImportedZoneTilingJobSaved>) eventCaptor.getValue();
    var importedZoneTilingJobSaved = events.get(0);
    assertEquals(importedJobId, importedZoneTilingJobSaved.getJobId());
    assertEquals(geoServerParameter, importedZoneTilingJobSaved.getGeoServerParameter());
    assertEquals(geoServerUrlDummy, importedZoneTilingJobSaved.getGeoServerUrl());
    assertEquals(bucketName, importedZoneTilingJobSaved.getBucketName());
    assertEquals(bucketPathPrefix, importedZoneTilingJobSaved.getBucketPathPrefix());
    assertEquals(startFrom, importedZoneTilingJobSaved.getStartFrom());
    assertEquals(endAt, importedZoneTilingJobSaved.getEndAt());
    assertEquals(job, actual);
  }

  @Test
  void dispatch_task_by_success_status_ko() {
    when(jobRepositoryMock.findById(JOB_ID_NOT_FOUND)).thenReturn(Optional.empty());
    when(jobRepositoryMock.findById(JOB_5_ID))
        .thenReturn(
            Optional.of(
                ZoneTilingJob.builder()
                    .statusHistory(
                        List.of(
                            JobStatus.builder()
                                .progression(FINISHED)
                                .health(SUCCEEDED)
                                .creationDatetime(now())
                                .build()))
                    .build()));

    assertThrows(
        NotFoundException.class, () -> subject.dispatchTasksBySuccessStatus(JOB_ID_NOT_FOUND));
    assertThrows(BadRequestException.class, () -> subject.dispatchTasksBySuccessStatus(JOB_5_ID));
  }

  @Test
  void dispatch_task_by_success_status_ok() {
    when(jobRepositoryMock.save(ArgumentMatchers.any()))
        .thenAnswer(
            (Answer<ZoneTilingJob>)
                invocationOnMock -> {
                  Object[] args = invocationOnMock.getArguments();
                  return (ZoneTilingJob) args[0];
                });
    when(jobRepositoryMock.findById(JOB_4_ID))
        .thenReturn(
            Optional.of(
                ZoneTilingJob.builder()
                    .statusHistory(
                        List.of(
                            JobStatus.builder()
                                .progression(PROCESSING)
                                .health(FAILED)
                                .creationDatetime(now())
                                .build()))
                    .build()));
    when(taskRepositoryMock.findAllByJobId(JOB_4_ID))
        .thenReturn(
            List.of(
                taskWithStatus(FINISHED, SUCCEEDED),
                taskWithStatus(FINISHED, SUCCEEDED),
                taskWithStatus(PENDING, UNKNOWN),
                taskWithStatus(PENDING, UNKNOWN),
                taskWithStatus(FINISHED, FAILED),
                taskWithStatus(PROCESSING, UNKNOWN)));

    FilteredTilingJob filteredZoneTilingJobs = subject.dispatchTasksBySuccessStatus(JOB_4_ID);

    var succeededJob = filteredZoneTilingJobs.getSucceededJob();
    var notSucceededJob = filteredZoneTilingJobs.getNotSucceededJob();
    assertEquals(FINISHED, succeededJob.getStatus().getProgression());
    assertEquals(SUCCEEDED, succeededJob.getStatus().getHealth());
    assertEquals(PENDING, notSucceededJob.getStatus().getProgression());
    assertEquals(UNKNOWN, notSucceededJob.getStatus().getHealth());

    var listEventCapture = ArgumentCaptor.forClass(List.class);
    verify(taskRepositoryMock, times(2)).saveAll(listEventCapture.capture());
    var succeededTasks = (List<TilingTask>) listEventCapture.getAllValues().get(0);
    var notSucceededTasks = (List<TilingTask>) listEventCapture.getAllValues().get(1);
    assertEquals(2, succeededTasks.size());
    assertTrue(succeededTasks.stream().allMatch(TilingTask::isSucceeded));
    assertEquals(4, notSucceededTasks.size());
    assertEquals(
        2L,
        notSucceededTasks.stream()
            .filter(
                task ->
                    PENDING.equals(task.getStatus().getProgression())
                        && UNKNOWN.equals(task.getStatus().getHealth()))
            .count());
    assertEquals(
        1L,
        notSucceededTasks.stream()
            .filter(
                task ->
                    FINISHED.equals(task.getStatus().getProgression())
                        && FAILED.equals(task.getStatus().getHealth()))
            .count());
    assertEquals(
        1L,
        notSucceededTasks.stream()
            .filter(
                task ->
                    PROCESSING.equals(task.getStatus().getProgression())
                        && UNKNOWN.equals(task.getStatus().getHealth()))
            .count());
  }

  @Test
  void read_task_statistics_ok() {
    when(jobRepositoryMock.findById(JOB_3_ID))
        .thenReturn(
            Optional.of(
                ZoneTilingJob.builder()
                    .statusHistory(
                        List.of(JobStatus.builder().progression(PROCESSING).health(FAILED).build()))
                    .build()));
    when(taskRepositoryMock.findAllByJobId(JOB_3_ID))
        .thenReturn(
            List.of(
                taskWithStatus(FINISHED, SUCCEEDED),
                taskWithStatus(FINISHED, SUCCEEDED),
                taskWithStatus(PENDING, UNKNOWN),
                taskWithStatus(PENDING, UNKNOWN),
                taskWithStatus(FINISHED, FAILED),
                taskWithStatus(PROCESSING, UNKNOWN)));

    TaskStatistic actual = subject.computeTaskStatistics(JOB_3_ID);

    var eventCapture = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock, times(1)).accept(eventCapture.capture());
    List<TaskStatisticRecomputingSubmitted> events = eventCapture.getValue();
    var taskStatisticRecomputingEvent = events.getFirst();
    assertEquals(JOB_3_ID, actual.getJobId());
    assertEquals(actual.getJobId(), taskStatisticRecomputingEvent.getJobId());
    assertEquals(FAILED, actual.getActualJobStatus().getHealth());
    assertEquals(PROCESSING, actual.getActualJobStatus().getProgression());
    assertTrue(actual.getTaskStatusStatistics().isEmpty());
  }

  @Test
  void retry_failed_tasks_not_found_ko() {
    when(jobRepositoryMock.findById(JOB_ID)).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> subject.retryFailedTask(JOB_ID));
  }

  @Test
  void retry_failed_tasks_all_tasks_not_finished_ko() {
    when(jobRepositoryMock.findById(JOB_ID))
        .thenReturn(Optional.of(ZoneTilingJob.builder().build()));
    when(jobRepositoryMock.findById(JOB2_ID))
        .thenReturn(Optional.of(ZoneTilingJob.builder().build()));
    when(taskRepositoryMock.findAllByJobId(JOB_ID))
        .thenReturn(
            List.of(taskWithStatus(FINISHED, SUCCEEDED), taskWithStatus(PROCESSING, UNKNOWN)));
    when(taskRepositoryMock.findAllByJobId(JOB2_ID))
        .thenReturn(
            List.of(taskWithStatus(FINISHED, SUCCEEDED), taskWithStatus(FINISHED, SUCCEEDED)));

    assertThrows(BadRequestException.class, () -> subject.retryFailedTask(JOB_ID));
    assertThrows(BadRequestException.class, () -> subject.retryFailedTask(JOB2_ID));
  }

  static TilingTask taskWithStatus(
      Status.ProgressionStatus progressionStatus, Status.HealthStatus healthStatus) {
    return TilingTask.builder()
        .statusHistory(
            List.of(
                TaskStatus.builder()
                    .id(randomUUID().toString())
                    .progression(progressionStatus)
                    .jobType(TILING)
                    .health(healthStatus)
                    .build()))
        .build();
  }

  static TilingTask taskWithStatus(
      Status.ProgressionStatus progressionStatus, Status.HealthStatus healthStatus, Parcel parcel) {
    return TilingTask.builder()
        .statusHistory(
            List.of(
                TaskStatus.builder()
                    .id(randomUUID().toString())
                    .progression(progressionStatus)
                    .jobType(TILING)
                    .health(healthStatus)
                    .build()))
        .parcels(parcel == null ? null : List.of(parcel))
        .build();
  }

  @Test
  void retry_failed_tasks_ok() {
    when(jobRepositoryMock.findById(JOB_ID)).thenReturn(Optional.of(new ZoneTilingJob()));
    when(jobRepositoryMock.save(ArgumentMatchers.any()))
        .thenAnswer(
            (Answer<ZoneTilingJob>)
                invocationOnMock -> {
                  Object[] args = invocationOnMock.getArguments();
                  return (ZoneTilingJob) args[0];
                });
    when(taskRepositoryMock.saveAll(ArgumentMatchers.any()))
        .thenReturn(
            List.of(
                TilingTask.builder()
                    .statusHistory(
                        List.of(
                            TaskStatus.builder()
                                .jobType(DETECTION)
                                .progression(FINISHED)
                                .health(FAILED)
                                .creationDatetime(now())
                                .build(),
                            TaskStatus.builder()
                                .jobType(DETECTION)
                                .progression(PENDING)
                                .health(RETRYING)
                                .creationDatetime(now())
                                .build()))
                    .build()));
    when(taskRepositoryMock.findAllByJobId(JOB_ID))
        .thenReturn(
            List.of(
                TilingTask.builder()
                    .statusHistory(
                        List.of(
                            TaskStatus.builder()
                                .id(randomUUID().toString())
                                .progression(FINISHED)
                                .jobType(DETECTION)
                                .health(SUCCEEDED)
                                .build()))
                    .build(),
                TilingTask.builder()
                    .statusHistory(
                        List.of(
                            TaskStatus.builder()
                                .id(randomUUID().toString())
                                .progression(FINISHED)
                                .jobType(DETECTION)
                                .health(FAILED)
                                .creationDatetime(now())
                                .build()))
                    .build()));

    ZoneTilingJob actual = subject.retryFailedTask(JOB_ID);

    assertEquals(PROCESSING, actual.getStatus().getProgression());
    assertEquals(RETRYING, actual.getStatus().getHealth());
  }

  public record StatisticResult(
      HealthStatusStatistic unknownPendingTask,
      HealthStatusStatistic unknownProcessingTask,
      HealthStatusStatistic succeededFinishedTask,
      HealthStatusStatistic failedFinishedTask,
      HealthStatusStatistic unknownFinishedTask) {}

  @NonNull
  public static ZoneTilingJobServiceTest.StatisticResult getResult(TaskStatistic actual) {
    var pendingTaskStatistic =
        actual.getTaskStatusStatistics().stream()
            .filter(statistic -> statistic.getProgressionStatus().equals(PENDING))
            .findFirst()
            .orElseThrow();
    var processingTaskStatistic =
        actual.getTaskStatusStatistics().stream()
            .filter(statistic -> statistic.getProgressionStatus().equals(PROCESSING))
            .findFirst()
            .orElseThrow();
    var finishedTaskStatistic =
        actual.getTaskStatusStatistics().stream()
            .filter(statistic -> statistic.getProgressionStatus().equals(FINISHED))
            .findFirst()
            .orElseThrow();
    var unknownPendingTask =
        pendingTaskStatistic.getHealthStatusStatistics().stream()
            .filter(healthStatistic -> healthStatistic.getHealthStatus().equals(UNKNOWN))
            .findFirst()
            .orElseThrow();
    var unknownProcessingTask =
        processingTaskStatistic.getHealthStatusStatistics().stream()
            .filter(healthStatistic -> healthStatistic.getHealthStatus().equals(UNKNOWN))
            .findFirst()
            .orElseThrow();
    var succeededFinishedTask =
        finishedTaskStatistic.getHealthStatusStatistics().stream()
            .filter(healthStatistic -> healthStatistic.getHealthStatus().equals(SUCCEEDED))
            .findFirst()
            .orElseThrow();
    var failedFinishedTask =
        finishedTaskStatistic.getHealthStatusStatistics().stream()
            .filter(healthStatistic -> healthStatistic.getHealthStatus().equals(FAILED))
            .findFirst()
            .orElseThrow();
    var unknownFinishedTask =
        finishedTaskStatistic.getHealthStatusStatistics().stream()
            .filter(healthStatistic -> healthStatistic.getHealthStatus().equals(UNKNOWN))
            .findFirst()
            .orElseThrow();
    return new StatisticResult(
        unknownPendingTask,
        unknownProcessingTask,
        succeededFinishedTask,
        failedFinishedTask,
        unknownFinishedTask);
  }
}
