package app.bpartners.geojobs.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.TileDetectionTaskCreated;
import app.bpartners.geojobs.endpoint.event.model.TileDetectionTaskCreatedFailed;
import app.bpartners.geojobs.endpoint.event.model.TileDetectionTaskSucceeded;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.service.detection.TileDetectionTaskStatusService;
import app.bpartners.geojobs.service.event.ExceptionToStringFunction;
import app.bpartners.geojobs.service.event.TileDetectionTaskCreatedConsumer;
import app.bpartners.geojobs.service.event.TileDetectionTaskCreatedFailedService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class TileDetectionTaskCreatedFailedServiceTest {
  EventProducer eventProducerMock = mock();
  TileDetectionTaskCreatedConsumer taskCreatedConsumerMock = mock();
  TileDetectionTaskStatusService taskStatusServiceMock = mock();
  TileDetectionTaskCreatedFailedService subject =
      new TileDetectionTaskCreatedFailedService(
          eventProducerMock,
          taskCreatedConsumerMock,
          taskStatusServiceMock,
          new ExceptionToStringFunction());

  @Test
  void task_consumed_ok() {
    TileDetectionTask tileDetectionTask = aTileDetectionTask();
    TileDetectionTaskCreated tileDetectionTaskCreated =
        TileDetectionTaskCreated.builder().tileDetectionTask(tileDetectionTask).build();

    assertDoesNotThrow(
        () -> subject.accept(new TileDetectionTaskCreatedFailed(tileDetectionTaskCreated, 1)));

    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(taskStatusServiceMock, times(0)).fail(tileDetectionTask);
    verify(taskCreatedConsumerMock, times(1)).accept(tileDetectionTaskCreated);
    verify(eventProducerMock, times(1)).accept(listCaptor.capture());
    var taskSucceededEvent = ((List<TileDetectionTaskSucceeded>) listCaptor.getValue()).getFirst();
    assertEquals(new TileDetectionTaskSucceeded(tileDetectionTask, null), taskSucceededEvent);
  }

  @Test
  void task_not_consumed_ok() {
    TileDetectionTask tileDetectionTask = aTileDetectionTask();
    TileDetectionTaskCreated tileDetectionTaskCreated =
        TileDetectionTaskCreated.builder().tileDetectionTask(tileDetectionTask).build();
    doThrow(RuntimeException.class).when(taskCreatedConsumerMock).accept(tileDetectionTaskCreated);

    assertDoesNotThrow(
        () -> subject.accept(new TileDetectionTaskCreatedFailed(tileDetectionTaskCreated, 1)));

    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(taskStatusServiceMock, times(0)).fail(tileDetectionTask);
    verify(taskCreatedConsumerMock, times(1)).accept(tileDetectionTaskCreated);
    verify(eventProducerMock, times(1)).accept(listCaptor.capture());
    assertInstanceOf(TileDetectionTaskCreatedFailed.class, listCaptor.getValue().getFirst());
  }

  @Test
  void max_attempt_nb_reached_ok() {
    TileDetectionTask tileDetectionTask = aTileDetectionTask();
    TileDetectionTaskCreated tileDetectionTaskCreated =
        TileDetectionTaskCreated.builder().tileDetectionTask(tileDetectionTask).build();

    assertDoesNotThrow(
        () -> subject.accept(new TileDetectionTaskCreatedFailed(tileDetectionTaskCreated, 4)));

    verify(taskStatusServiceMock, times(1)).fail(tileDetectionTask);
    verify(taskCreatedConsumerMock, times(0)).accept(tileDetectionTaskCreated);
    verify(eventProducerMock, times(0)).accept(any());
  }

  private TileDetectionTask aTileDetectionTask() {
    List<TaskStatus> statusHistory = new ArrayList<>();
    statusHistory.add(
        TaskStatus.builder()
            .progression(Status.ProgressionStatus.PROCESSING)
            .health(Status.HealthStatus.UNKNOWN)
            .creationDatetime(Instant.now())
            .build());
    TileDetectionTask tileDetectionTask =
        TileDetectionTask.builder().statusHistory(statusHistory).build();
    return tileDetectionTask;
  }
}
