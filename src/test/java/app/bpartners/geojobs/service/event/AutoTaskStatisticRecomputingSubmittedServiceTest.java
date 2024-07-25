package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.endpoint.event.EventStack.EVENT_STACK_2;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.*;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.*;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.AutoTaskStatisticRecomputingSubmitted;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.*;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class AutoTaskStatisticRecomputingSubmittedServiceTest {
  private static final String TILING_JOB_ID = "tilingJobId";
  private static final String DETECTION_JOB_ID = "detection_job_id";
  private static final String NOT_FOUND_JOB = "notFoundJob";
  private static final String NOT_FINISHED_JOB = "notFinishedJob";
  private static final int MAX_ATTEMPT_NB = 6;
  private static final long SOME_BACK_OFF_VALUE = 1L;
  EventProducer eventProducerMock = mock();
  ZoneDetectionJobRepository zoneDetectionRepositoryMock = mock();
  ParcelDetectionTaskRepository parcelDetectionTaskRepositoryMock = mock();
  ZoneTilingJobRepository tilingJobRepositoryMock = mock();
  TilingTaskRepository tilingTaskRepositoryMock = mock();
  TaskStatisticRepository taskStatisticRepositoryMock = mock();
  AutoTaskStatisticRecomputingSubmittedService subject =
      new AutoTaskStatisticRecomputingSubmittedService(
          eventProducerMock,
          zoneDetectionRepositoryMock,
          parcelDetectionTaskRepositoryMock,
          tilingJobRepositoryMock,
          tilingTaskRepositoryMock,
          taskStatisticRepositoryMock);

  @Test
  void accept_tiling_ok() {
    when(tilingJobRepositoryMock.findById(TILING_JOB_ID))
        .thenReturn(Optional.of(aZTJ(TILING_JOB_ID, PROCESSING, UNKNOWN)));

    subject.accept(new AutoTaskStatisticRecomputingSubmitted(TILING_JOB_ID));

    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(tilingJobRepositoryMock, times(2)).findById(TILING_JOB_ID);
    verify(taskStatisticRepositoryMock, times(1)).save(any());
    verify(eventProducerMock, times(1)).accept(listCaptor.capture());
    var newEvent = ((List<AutoTaskStatisticRecomputingSubmitted>) listCaptor.getValue()).getFirst();
    assertEquals(
        AutoTaskStatisticRecomputingSubmitted.builder()
            .jobId(TILING_JOB_ID)
            .attemptNb(1)
            .maxConsumerDurationValue(300L)
            .maxConsumerBackoffBetweenRetriesDurationValue(180L * 2)
            .build(),
        newEvent);
    assertEquals(EVENT_STACK_2, newEvent.getEventStack());
    assertEquals(Duration.ofSeconds(300L), newEvent.maxConsumerDuration());
    assertEquals(Duration.ofSeconds(180L * 2), newEvent.maxConsumerBackoffBetweenRetries());
  }

  @Test
  void accept_detection_ok() {
    when(zoneDetectionRepositoryMock.findById(DETECTION_JOB_ID))
        .thenReturn(Optional.of(aZDJ(DETECTION_JOB_ID, PROCESSING, UNKNOWN)));

    subject.accept(new AutoTaskStatisticRecomputingSubmitted(DETECTION_JOB_ID));

    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(zoneDetectionRepositoryMock, times(2)).findById(DETECTION_JOB_ID);
    verify(taskStatisticRepositoryMock, times(1)).save(any());
    verify(eventProducerMock, times(1)).accept(listCaptor.capture());
    var newEvent = ((List<AutoTaskStatisticRecomputingSubmitted>) listCaptor.getValue()).getFirst();
    assertEquals(
        AutoTaskStatisticRecomputingSubmitted.builder()
            .jobId(DETECTION_JOB_ID)
            .attemptNb(1)
            .maxConsumerDurationValue(300L)
            .maxConsumerBackoffBetweenRetriesDurationValue(180L * 2)
            .build(),
        newEvent);
  }

  @Test
  void accept_detection_ko() {
    when(tilingJobRepositoryMock.findById(NOT_FOUND_JOB)).thenReturn(Optional.empty());
    when(zoneDetectionRepositoryMock.findById(NOT_FOUND_JOB)).thenReturn(Optional.empty());

    assertThrows(
        NotFoundException.class,
        () -> subject.accept(new AutoTaskStatisticRecomputingSubmitted(NOT_FOUND_JOB)));
  }

  @Test
  void accept_finished_job_or_max_attempt_reached_ok() {
    when(tilingJobRepositoryMock.findById(NOT_FINISHED_JOB))
        .thenReturn(Optional.of(aZTJ(NOT_FINISHED_JOB, PENDING, UNKNOWN)));
    when(tilingJobRepositoryMock.findById(TILING_JOB_ID))
        .thenReturn(Optional.of(aZTJ(TILING_JOB_ID, FINISHED, SUCCEEDED)));
    when(zoneDetectionRepositoryMock.findById(DETECTION_JOB_ID))
        .thenReturn(Optional.of(aZDJ(DETECTION_JOB_ID, FINISHED, FAILED)));

    subject.accept(
        new AutoTaskStatisticRecomputingSubmitted(
            NOT_FINISHED_JOB, SOME_BACK_OFF_VALUE, MAX_ATTEMPT_NB));
    subject.accept(new AutoTaskStatisticRecomputingSubmitted(TILING_JOB_ID));
    subject.accept(new AutoTaskStatisticRecomputingSubmitted(DETECTION_JOB_ID));

    verify(eventProducerMock, times(0)).accept(any());
    verify(taskStatisticRepositoryMock, times(3)).save(any());
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

  private static ZoneDetectionJob aZDJ(
      String jobId, Status.ProgressionStatus progressionStatus, Status.HealthStatus healthStatus) {
    return ZoneDetectionJob.builder()
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
