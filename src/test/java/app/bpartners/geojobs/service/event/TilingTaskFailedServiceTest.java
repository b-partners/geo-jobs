package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PROCESSING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.tile.TilingTaskFailed;
import app.bpartners.geojobs.endpoint.event.model.tile.TilingTaskSucceeded;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.job.service.TaskStatusService;
import app.bpartners.geojobs.repository.model.tiling.TilingTask;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TilingTaskFailedServiceTest {
  TaskStatusService<TilingTask> taskStatusService = mock();
  TilingTaskConsumer tilingTaskConsumer = mock();
  EventProducer eventProducer = mock();
  TilingTaskFailedService subject =
      new TilingTaskFailedService(
          taskStatusService, tilingTaskConsumer, eventProducer, new ExceptionToStringFunction());

  @Test
  void fail_if_max_attempt_reached() {
    var task = new TilingTask();
    task.setStatusHistory(
        List.of(
            TaskStatus.builder()
                .progression(PROCESSING)
                .health(UNKNOWN)
                .message("An error message")
                .build()));
    var taskFailed = TilingTaskFailed.builder().task(task).attemptNb(4).build();

    subject.accept(taskFailed);

    var eventsCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducer, times(0)).accept(eventsCaptor.capture());
    verify(taskStatusService, times(1)).fail(task);
  }

  @Test
  void retry_if_max_attempt_not_reached() {
    var task = new TilingTask();
    var taskFailed = TilingTaskFailed.builder().task(task).attemptNb(2).build();
    var failingSubject =
        new TilingTaskFailedService(
            taskStatusService,
            new TilingTaskConsumer(mock(), mock()) {
              @Override
              public void accept(TilingTask tilingTask) {
                throw new RuntimeException();
              }
            },
            eventProducer,
            new ExceptionToStringFunction());

    failingSubject.accept(taskFailed);

    var eventsCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducer, times(1)).accept(eventsCaptor.capture());
    var event = (TilingTaskFailed) eventsCaptor.getValue().get(0);
    assertEquals(3, event.getAttemptNb());
    var statusInEvent = event.getTask().getStatus();
    assertEquals(PROCESSING, statusInEvent.getProgression());
    assertEquals(UNKNOWN, statusInEvent.getHealth());
  }

  @Test
  void send_tilingSucceed_on_success() {
    var taskFailed = TilingTaskFailed.builder().task(new TilingTask()).attemptNb(3).build();

    subject.accept(taskFailed);

    var eventsCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducer, times(1)).accept(eventsCaptor.capture());
    var statusInEvent =
        ((TilingTaskSucceeded) eventsCaptor.getValue().get(0)).getTask().getStatus();
    assertEquals(FINISHED, statusInEvent.getProgression());
    assertEquals(SUCCEEDED, statusInEvent.getHealth());
  }
}
