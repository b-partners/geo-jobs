package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.*;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.*;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.ZTJStatusRecomputingSubmitted;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.job.repository.TaskRepository;
import app.bpartners.geojobs.job.service.TaskStatusService;
import app.bpartners.geojobs.repository.model.tiling.TilingTask;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.tiling.ZoneTilingJobService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class ZTJStatusRecomputingSubmittedServiceTest {
  ZoneTilingJobService tilingJobServiceMock = mock();
  EventProducer eventProducerMock = mock();
  TaskStatusService<TilingTask> taskStatusServiceMock = mock();
  TaskRepository<TilingTask> taskRepositoryMock = mock();
  ZTJStatusRecomputingSubmittedService subject =
      new ZTJStatusRecomputingSubmittedService(
          tilingJobServiceMock, eventProducerMock, taskStatusServiceMock, taskRepositoryMock);

  @Test
  void accept_max_attempt_reached() {
    String processingJob = "jobId";
    ZoneTilingJob zoneTilingJob = aZTJ(processingJob, PROCESSING, UNKNOWN);

    when(tilingJobServiceMock.findById(processingJob)).thenReturn(zoneTilingJob);
    when(tilingJobServiceMock.recomputeStatus(zoneTilingJob)).thenReturn(zoneTilingJob);
    when(taskRepositoryMock.findAllByJobId(processingJob))
        .thenReturn(
            List.of(
                aTilingTask(PROCESSING, UNKNOWN),
                aTilingTask(PROCESSING, UNKNOWN),
                aTilingTask(FINISHED, FAILED),
                aTilingTask(FINISHED, SUCCEEDED)));

    subject.accept(new ZTJStatusRecomputingSubmitted(processingJob, 180L, 6));

    verify(eventProducerMock, times(0)).accept(any());
    verify(taskRepositoryMock, times(1)).findAllByJobId(processingJob);
    verify(taskStatusServiceMock, times(2)).fail(any());
    var jobCaptor = ArgumentCaptor.forClass(ZoneTilingJob.class);
    verify(tilingJobServiceMock, times(2)).recomputeStatus(jobCaptor.capture());
    assertFalse(jobCaptor.getValue().isFinished());
  }

  private static TilingTask aTilingTask(
      Status.ProgressionStatus progressionStatus, Status.HealthStatus healthStatus) {
    return TilingTask.builder()
        .id(randomUUID().toString())
        .statusHistory(
            List.of(
                TaskStatus.builder()
                    .progression(progressionStatus)
                    .health(healthStatus)
                    .creationDatetime(now())
                    .build()))
        .build();
  }

  @Test
  void accept_pending_ok() {
    String pendingJobId = "jobId";
    ZoneTilingJob zoneTilingJob = aZTJ(pendingJobId, PENDING, UNKNOWN);

    when(tilingJobServiceMock.findById(pendingJobId)).thenReturn(zoneTilingJob);
    when(tilingJobServiceMock.recomputeStatus(zoneTilingJob)).thenReturn(zoneTilingJob);

    subject.accept(new ZTJStatusRecomputingSubmitted(pendingJobId));

    ArgumentCaptor<List<ZTJStatusRecomputingSubmitted>> listCaptor =
        ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock, times(1)).accept(listCaptor.capture());
    var event = listCaptor.getValue().getFirst();
    assertEquals(new ZTJStatusRecomputingSubmitted(pendingJobId, 10L, 1), event);
  }

  @Test
  void accept_finished_ok() {
    String succeededJobId = "succeededJobId";
    ZoneTilingJob succeededJob = aZTJ(succeededJobId, FINISHED, SUCCEEDED);
    String failedJobId = "failedJobId";
    ZoneTilingJob failedJob = aZTJ(failedJobId, FINISHED, FAILED);
    when(tilingJobServiceMock.findById(succeededJobId)).thenReturn(succeededJob);
    when(tilingJobServiceMock.recomputeStatus(succeededJob)).thenReturn(succeededJob);
    when(tilingJobServiceMock.findById(failedJobId)).thenReturn(failedJob);
    when(tilingJobServiceMock.recomputeStatus(failedJob)).thenReturn(failedJob);

    subject.accept(new ZTJStatusRecomputingSubmitted(succeededJobId));
    subject.accept(new ZTJStatusRecomputingSubmitted(failedJobId));

    ArgumentCaptor<List<ZTJStatusRecomputingSubmitted>> listCaptor =
        ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock, times(0)).accept(listCaptor.capture());
  }

  @Test
  void accept_do_nothing_ok() {
    String jobId = "jobId";
    ZoneTilingJob job = aZTJ(jobId, PROCESSING, UNKNOWN);
    when(tilingJobServiceMock.findById(jobId)).thenReturn(job);
    when(tilingJobServiceMock.recomputeStatus(job)).thenReturn(job);

    subject.accept(new ZTJStatusRecomputingSubmitted(jobId, 256L, 8));

    ArgumentCaptor<List<ZTJStatusRecomputingSubmitted>> listCaptor =
        ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock, times(0)).accept(listCaptor.capture());
  }

  private static ZoneTilingJob aZTJ(
      String jobId, Status.ProgressionStatus progressionStatus, Status.HealthStatus healthStatus) {
    return ZoneTilingJob.builder()
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
                    .build()))
        .build();
  }
}
