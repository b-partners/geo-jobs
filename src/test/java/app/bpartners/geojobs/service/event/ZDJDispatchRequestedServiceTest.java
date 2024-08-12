package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.*;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.*;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

import app.bpartners.geojobs.endpoint.event.model.ZDJDispatchRequested;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.GeoServerParameter;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.job.model.Task;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.repository.ParcelDetectionTaskRepository;
import app.bpartners.geojobs.repository.TileDetectionTaskRepository;
import app.bpartners.geojobs.repository.ZoneDetectionJobRepository;
import app.bpartners.geojobs.repository.model.*;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionJob;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionTask;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.JobFilteredMailer;
import app.bpartners.geojobs.service.KeyPredicateFunction;
import app.bpartners.geojobs.service.TaskToJobConverter;
import app.bpartners.geojobs.service.detection.ParcelDetectionJobService;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.stubbing.Answer;

public class ZDJDispatchRequestedServiceTest {
  private static final String JOB_ID = "jobId";
  private static final String PDJ_ID_1 = "pdjId1";
  private static final String PDJ_2 = "pdjId2";
  private static final String JOB_WITH_EXCEPTION_ID = "jobWithExceptionId";
  ParcelDetectionTaskRepository taskRepositoryMock = mock();
  TileDetectionTaskRepository tileDetectionTaskRepositoryMock = mock();
  ParcelDetectionJobService parcelDetectionJobServiceMock = mock();
  ZoneDetectionJobRepository zoneDetectionJobRepositoryMock = mock();
  TaskToJobConverter<ParcelDetectionTask, ParcelDetectionJob> parcelJobConverter =
      new TaskToJobConverter<>();
  KeyPredicateFunction keyPredicateFunction = new KeyPredicateFunction();
  JobFilteredMailer<ZoneDetectionJob> detectionFilteredMailer = mock();
  ZDJDispatchRequestedService subject =
      new ZDJDispatchRequestedService(
          zoneDetectionJobRepositoryMock,
          taskRepositoryMock,
          detectionFilteredMailer,
          parcelJobConverter,
          keyPredicateFunction,
          parcelDetectionJobServiceMock,
          tileDetectionTaskRepositoryMock);

  @Test
  void dispatch_task_by_success_status_ko() {
    String succeededJobId = "succeededJobId";
    String notSucceededJobId = "notSucceededJobId";
    when(zoneDetectionJobRepositoryMock.findById(JOB_WITH_EXCEPTION_ID))
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
    when(taskRepositoryMock.findAllByJobId(JOB_WITH_EXCEPTION_ID))
        .thenReturn(
            List.of(
                ParcelDetectionTask.builder()
                    .statusHistory(
                        List.of(
                            TaskStatus.builder().progression(FINISHED).health(SUCCEEDED).build()))
                    .build()));

    assertThrows(
        ApiException.class,
        () ->
            subject.accept(
                new ZDJDispatchRequested(
                    ZoneDetectionJob.builder().id(JOB_WITH_EXCEPTION_ID).build(),
                    succeededJobId,
                    notSucceededJobId)));
  }

  @SneakyThrows
  @Test
  void dispatch_task_by_success_status_ok() {
    ZoneDetectionJob detectionJob = detectionJob();
    String succeededJobId = "succeededJobId";
    String notSucceededJobId = "notSucceededJobId";
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
    when(zoneDetectionJobRepositoryMock.save(ArgumentMatchers.any()))
        .thenAnswer(
            (Answer<ZoneDetectionJob>)
                invocationOnMock -> {
                  Object[] args = invocationOnMock.getArguments();
                  return (ZoneDetectionJob) args[0];
                });
    when(zoneDetectionJobRepositoryMock.findById(JOB_ID)).thenReturn(Optional.of(detectionJob));
    when(taskRepositoryMock.findAllByJobId(JOB_ID))
        .thenReturn(List.of(originalFinishedParcelTask, originalNotFinishedParcelTask));
    when(tileDetectionTaskRepositoryMock.findAllByJobId(PDJ_ID_1)).thenReturn(tilesPDJ1);
    when(tileDetectionTaskRepositoryMock.findAllByJobId(PDJ_2)).thenReturn(tilesPDJ2);
    when(parcelDetectionJobServiceMock.save(any(), any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));

    assertDoesNotThrow(
        () ->
            subject.accept(
                new ZDJDispatchRequested(detectionJob, succeededJobId, notSucceededJobId)));

    var filteredJobCaptor = ArgumentCaptor.forClass(FilteredDetectionJob.class);
    verify(detectionFilteredMailer, times(1)).accept(filteredJobCaptor.capture());
    var actual = filteredJobCaptor.getValue();
    var succeededJob = actual.getSucceededJob();
    var notSucceededJob = actual.getNotSucceededJob();
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

  private static ZoneDetectionJob detectionJob() {
    return ZoneDetectionJob.builder()
        .id(JOB_ID)
        .statusHistory(
            List.of(
                JobStatus.builder()
                    .progression(PROCESSING)
                    .health(FAILED)
                    .creationDatetime(now())
                    .build()))
        .build();
  }

  @SneakyThrows
  private static ParcelDetectionTask parcelDetectionTask(
      String asJobId,
      Status.ProgressionStatus progressionStatus,
      Status.HealthStatus healthStatus,
      List<Tile> tiles) {
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
