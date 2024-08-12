package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.*;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.*;
import static app.bpartners.geojobs.repository.model.GeoJobType.DETECTION;
import static app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.DetectionType.HUMAN;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.TaskStatisticRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.GeoServerParameter;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.job.model.Task;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.job.model.statistic.TaskStatistic;
import app.bpartners.geojobs.job.repository.JobStatusRepository;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.repository.ParcelDetectionTaskRepository;
import app.bpartners.geojobs.repository.TileDetectionTaskRepository;
import app.bpartners.geojobs.repository.ZoneDetectionJobRepository;
import app.bpartners.geojobs.repository.model.FilteredDetectionJob;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.ParcelContent;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionJob;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionTask;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.detection.ParcelDetectionJobService;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import jakarta.persistence.EntityManager;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.stubbing.Answer;
import org.springframework.data.jpa.repository.JpaRepository;

@Slf4j
public class ZoneDetectionJobServiceTest {
  public static final String JOB_ID = "jobId";
  public static final String JOB2_ID = "job2Id";
  public static final String PDJ_2 = "pdjId2";
  public static final String PDJ_ID_1 = "pdjId1";
  public static final String JOB_3_ID = "job3_id";
  private static final String JOB_4_ID = "job4_id";
  private static final String JOB_ID_NOT_FOUND = "job_id_not_found";
  public static final String JOB_5_ID = "job5_id";
  JpaRepository<ZoneDetectionJob, String> jobRepositoryMock = mock();
  JobStatusRepository jobStatusRepositoryMock = mock();
  ParcelDetectionTaskRepository taskRepositoryMock = mock();
  EventProducer eventProducerMock = mock();
  EntityManager entityManagerMock = mock();
  TileDetectionTaskRepository tileDetectionTaskRepositoryMock = mock();
  ZoneDetectionJobRepository zoneDetectionJobRepositoryMock = mock();
  TaskToJobConverter<ParcelDetectionTask, ParcelDetectionJob> parcelJobConverter =
      new TaskToJobConverter<>();
  KeyPredicateFunction keyPredicateFunction = new KeyPredicateFunction();
  ParcelDetectionJobService parcelDetectionJobServiceMock = mock();
  ZoneDetectionJobService subject =
      new ZoneDetectionJobService(
          jobRepositoryMock,
          jobStatusRepositoryMock,
          mock(),
          taskRepositoryMock,
          eventProducerMock,
          mock(),
          mock(),
          mock(),
          mock(),
          mock(),
          zoneDetectionJobRepositoryMock,
          mock(),
          parcelJobConverter,
          keyPredicateFunction,
          parcelDetectionJobServiceMock,
          tileDetectionTaskRepositoryMock);

  @BeforeEach
  void setUp() {
    doNothing().when(entityManagerMock).detach(any());
    subject.setEm(entityManagerMock);
  }

  /*
  @Test
  void retry_failed_tasks_not_found_ko() {
    when(jobRepositoryMock.findById(JOB_ID)).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> subject.retryFailedTask(JOB_ID));
  }


  @Test
  void retry_failed_tasks_all_tasks_not_finished_ko() {
    when(jobRepositoryMock.findById(JOB2_ID))
        .thenReturn(Optional.of(ZoneDetectionJob.builder().build()));
    when(taskRepositoryMock.findAllByJobId(JOB2_ID))
        .thenReturn(
            List.of(
                ParcelDetectionTask.builder()
                    .id(PARENT_TASK_ID_2)
                    .statusHistory(
                        List.of(
                            TaskStatus.builder()
                                .id(randomUUID().toString())
                                .progression(FINISHED)
                                .jobType(DETECTION)
                                .health(SUCCEEDED)
                                .build()))
                    .build()));
    when(tileDetectionTaskRepositoryMock.findAllByParentTaskId(PARENT_TASK_ID_2))
        .thenReturn(
            List.of(
                TileDetectionTask.builder()
                    .statusHistory(
                        List.of(
                            TaskStatus.builder()
                                .id(randomUUID().toString())
                                .progression(FINISHED)
                                .jobType(DETECTION)
                                .health(SUCCEEDED)
                                .build()))
                    .build(),
                TileDetectionTask.builder()
                    .statusHistory(
                        List.of(
                            TaskStatus.builder()
                                .id(randomUUID().toString())
                                .progression(FINISHED)
                                .jobType(DETECTION)
                                .health(SUCCEEDED)
                                .build()))
                    .build()));

    assertThrows(BadRequestException.class, () -> subject.retryFailedTask(JOB2_ID));
  }

  @Test
  void retry_failed_tasks_ok() {
    when(jobRepositoryMock.findById(JOB_ID)).thenReturn(Optional.of(new ZoneDetectionJob()));
    when(jobRepositoryMock.save(ArgumentMatchers.any()))
        .thenAnswer(
            (Answer<ZoneDetectionJob>)
                invocationOnMock -> {
                  Object[] args = invocationOnMock.getArguments();
                  return (ZoneDetectionJob) args[0];
                });
    when(taskRepositoryMock.findAllByJobId(JOB_ID))
        .thenReturn(
            List.of(
                ParcelDetectionTask.builder()
                    .id(PARENT_TASK_ID_1)
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
    when(taskRepositoryMock.saveAll(ArgumentMatchers.any()))
        .thenReturn(
            List.of(
                ParcelDetectionTask.builder()
                    .id(PARENT_TASK_ID_1)
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
    when(tileDetectionTaskRepositoryMock.findAllByParentTaskId(PARENT_TASK_ID_1))
        .thenReturn(
            List.of(
                TileDetectionTask.builder()
                    .statusHistory(
                        List.of(
                            TaskStatus.builder()
                                .id(randomUUID().toString())
                                .progression(FINISHED)
                                .jobType(DETECTION)
                                .health(SUCCEEDED)
                                .build()))
                    .build(),
                TileDetectionTask.builder()
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

    ZoneDetectionJob actual = subject.retryFailedTask(JOB_ID);

    assertEquals(PROCESSING, actual.getStatus().getProgression());
    assertEquals(RETRYING, actual.getStatus().getHealth());
  }*/

  @Test
  void dispatch_task_by_success_status_ko() {
    when(jobRepositoryMock.findById(JOB_ID_NOT_FOUND)).thenReturn(Optional.empty());
    when(jobRepositoryMock.findById(JOB_5_ID))
        .thenReturn(
            Optional.of(
                ZoneDetectionJob.builder()
                    .statusHistory(
                        List.of(
                            JobStatus.builder()
                                .progression(FINISHED)
                                .health(SUCCEEDED)
                                .creationDatetime(now())
                                .build()))
                    .build()));
    when(taskRepositoryMock.findAllByJobId(JOB_5_ID))
        .thenReturn(
            List.of(
                ParcelDetectionTask.builder()
                    .statusHistory(
                        List.of(
                            TaskStatus.builder().progression(FINISHED).health(SUCCEEDED).build()))
                    .build()));

    assertThrows(
        NotFoundException.class, () -> subject.dispatchTasksBySucceededStatus(JOB_ID_NOT_FOUND));
    assertThrows(BadRequestException.class, () -> subject.dispatchTasksBySucceededStatus(JOB_5_ID));
  }

  @SneakyThrows
  @Test
  void dispatch_task_by_success_status_ok() {
    when(jobRepositoryMock.save(ArgumentMatchers.any()))
        .thenAnswer(
            (Answer<ZoneDetectionJob>)
                invocationOnMock -> {
                  Object[] args = invocationOnMock.getArguments();
                  return (ZoneDetectionJob) args[0];
                });
    when(jobRepositoryMock.findById(JOB_4_ID))
        .thenReturn(
            Optional.of(
                ZoneDetectionJob.builder()
                    .id(JOB_4_ID)
                    .statusHistory(
                        List.of(
                            JobStatus.builder()
                                .progression(PROCESSING)
                                .health(FAILED)
                                .creationDatetime(now())
                                .build()))
                    .build()));
    List<TileDetectionTask> tilesPDJ1 =
        List.of(aTileDetectionTask("tileBucket1", FINISHED, SUCCEEDED, PDJ_ID_1));
    List<TileDetectionTask> tilesPDJ2 =
        List.of(
            aTileDetectionTask("tileBucket2", FINISHED, SUCCEEDED, PDJ_2),
            aTileDetectionTask("tileBucket3", FINISHED, FAILED, PDJ_2),
            aTileDetectionTask("tileBucket4", PROCESSING, UNKNOWN, PDJ_2));
    ParcelDetectionTask originalFinishedParcelTask =
        parcelDetectionTask(
            PDJ_ID_1,
            FINISHED,
            SUCCEEDED,
            List.of(Tile.builder().bucketPath("tileBucket1").build()));
    ParcelDetectionTask originalNotFinishedParcelTask =
        parcelDetectionTask(
            PDJ_2,
            PROCESSING,
            FAILED,
            List.of(
                Tile.builder().bucketPath("tileBucket2").build(),
                Tile.builder().bucketPath("tileBucket3").build(),
                Tile.builder().bucketPath("tileBucket4").build()));
    when(taskRepositoryMock.findAllByJobId(JOB_4_ID))
        .thenReturn(List.of(originalFinishedParcelTask, originalNotFinishedParcelTask));
    when(tileDetectionTaskRepositoryMock.findAllByJobId(PDJ_ID_1)).thenReturn(tilesPDJ1);
    when(tileDetectionTaskRepositoryMock.findAllByJobId(PDJ_2)).thenReturn(tilesPDJ2);
    when(parcelDetectionJobServiceMock.save(any(), any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));

    FilteredDetectionJob filteredZoneTilingJobs = subject.dispatchTasksBySucceededStatus(JOB_4_ID);

    var succeededJob = filteredZoneTilingJobs.getSucceededJob();
    var notSucceededJob = filteredZoneTilingJobs.getNotSucceededJob();
    assertEquals(FINISHED, succeededJob.getStatus().getProgression());
    assertEquals(SUCCEEDED, succeededJob.getStatus().getHealth());
    assertEquals(PENDING, notSucceededJob.getStatus().getProgression());
    assertEquals(UNKNOWN, notSucceededJob.getStatus().getHealth());

    var listEventCapture = ArgumentCaptor.forClass(List.class);
    verify(taskRepositoryMock, times(2)).saveAll(listEventCapture.capture());
    var succeededTasks = (List<ParcelDetectionTask>) listEventCapture.getAllValues().get(0);
    var notSucceededTasks = (List<ParcelDetectionTask>) listEventCapture.getAllValues().get(1);
    assertEquals(1, succeededTasks.size());
    assertTrue(succeededTasks.stream().allMatch(ParcelDetectionTask::isSucceeded));
    assertEquals(1, notSucceededTasks.size());

    var parcelDetectionJobCaptor = ArgumentCaptor.forClass(ParcelDetectionJob.class);
    verify(parcelDetectionJobServiceMock, times(2))
        .save(parcelDetectionJobCaptor.capture(), listEventCapture.capture());
    var finishedPDJ = parcelDetectionJobCaptor.getAllValues().getFirst();
    var notFinishedPDJ = parcelDetectionJobCaptor.getAllValues().getLast();
    var finishedTileDetectionTasks =
        ((List<TileDetectionTask>) listEventCapture.getAllValues().get(2));
    var notFinishedTileDetectionTasks =
        ((List<TileDetectionTask>) listEventCapture.getAllValues().get(3));
    assertTrue(finishedPDJ.isSucceeded());
    assertTrue(notFinishedPDJ.isPending());
    assertEquals(1, finishedTileDetectionTasks.size());
    assertEquals(2, notFinishedTileDetectionTasks.size());
    assertTrue(finishedTileDetectionTasks.stream().allMatch(Task::isSucceeded));
    assertTrue(notFinishedTileDetectionTasks.stream().allMatch(Task::isPending));
    assertTrue(
        finishedTileDetectionTasks.stream()
            .map(tileDetectionTask -> tileDetectionTask.getTile().getBucketPath())
            .toList()
            .contains("tileBucket1"));
    assertTrue(
        notFinishedTileDetectionTasks.stream()
            .map(tileDetectionTask -> tileDetectionTask.getTile().getBucketPath())
            .toList()
            .containsAll(List.of("tileBucket3", "tileBucket4")));
  }

  @SneakyThrows
  private static ParcelDetectionTask parcelDetectionTask(
      String asJobId,
      Status.ProgressionStatus progressionStatus,
      Status.HealthStatus healthStatus,
      List<Tile> tiles)
      throws MalformedURLException {
    return ParcelDetectionTask.builder()
        .asJobId(asJobId)
        .parcels(
            List.of(
                Parcel.builder()
                    .id(randomUUID().toString())
                    .parcelContent(
                        ParcelContent.builder()
                            .id(randomUUID().toString())
                            .geoServerUrl(new URI("http:/dummy.com").toURL())
                            .geoServerParameter(new GeoServerParameter())
                            .feature(new Feature())
                            .tiles(tiles)
                            .build())
                    .build()))
        .statusHistory(
            List.of(
                TaskStatus.builder().progression(progressionStatus).health(healthStatus).build()))
        .build();
  }

  @Test
  void process_zdj_ko() {
    when(jobRepositoryMock.findById(JOB_4_ID))
        .thenReturn(
            Optional.of(
                ZoneDetectionJob.builder()
                    .statusHistory(
                        List.of(
                            JobStatus.builder().progression(PROCESSING).health(UNKNOWN).build()))
                    .zoneTilingJob(ZoneTilingJob.builder().id("zoneTilingJobId").build())
                    .build()));
    when(zoneDetectionJobRepositoryMock.findAllByZoneTilingJob_Id("zoneTilingJobId"))
        .thenReturn(List.of(ZoneDetectionJob.builder().detectionType(HUMAN).build()));

    assertThrows(
        NotImplementedException.class,
        () ->
            subject.fireTasks(
                JOB_4_ID,
                List.of(
                    DetectableObjectConfiguration.builder()
                        .bucketStorageName("bucketStorageName")
                        .build(),
                    DetectableObjectConfiguration.builder()
                        .bucketStorageName("otherBucketStorageName")
                        .build())));
  }

  @Test
  void read_task_statistics_ok() {
    when(jobRepositoryMock.findById(JOB_3_ID))
        .thenReturn(
            Optional.of(
                ZoneDetectionJob.builder()
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

  static ParcelDetectionTask taskWithStatus(
      Status.ProgressionStatus progressionStatus, Status.HealthStatus healthStatus) {
    return taskWithStatus(progressionStatus, healthStatus, null);
  }

  static ParcelDetectionTask taskWithStatus(
      Status.ProgressionStatus progressionStatus, Status.HealthStatus healthStatus, Parcel parcel) {
    return ParcelDetectionTask.builder()
        .statusHistory(
            List.of(
                TaskStatus.builder()
                    .id(randomUUID().toString())
                    .progression(progressionStatus)
                    .jobType(DETECTION)
                    .health(healthStatus)
                    .build()))
        .parcels(parcel == null ? null : List.of(parcel))
        .build();
  }

  private TileDetectionTask aTileDetectionTask(
      String tileBucketPath,
      Status.ProgressionStatus progressionStatus,
      Status.HealthStatus healthStatus,
      String parcelJobId) {
    List<TaskStatus> statusHistory = new ArrayList<>();
    statusHistory.add(
        TaskStatus.builder()
            .progression(progressionStatus)
            .health(healthStatus)
            .creationDatetime(now())
            .build());
    return TileDetectionTask.builder()
        .id(tileBucketPath)
        .jobId(parcelJobId)
        .tile(Tile.builder().bucketPath(tileBucketPath).build())
        .statusHistory(statusHistory)
        .build();
  }
}
