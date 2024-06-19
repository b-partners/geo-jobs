package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.*;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.DetectionTaskStatusChanged;
import app.bpartners.geojobs.endpoint.event.model.ParcelDetectionTaskSucceeded;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.job.service.TaskStatusService;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionTask;
import app.bpartners.geojobs.service.StatusChangedHandler;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class ParcelDetectionTaskStatusChangedServiceTest {
  EventProducer eventProducerMock = mock();
  TaskStatusService<ParcelDetectionTask> taskStatusServiceMock = mock();
  StatusChangedHandler statusChangedHandler = new StatusChangedHandler();
  DetectionTaskStatusChangedService subject =
      new DetectionTaskStatusChangedService(
          eventProducerMock, taskStatusServiceMock, statusChangedHandler);

  @Test
  void finished_succeeded_task_ok() {
    ParcelDetectionTask oldTask =
        ParcelDetectionTask.builder()
            .statusHistory(
                List.of(TaskStatus.builder().progression(PROCESSING).health(UNKNOWN).build()))
            .build();
    ParcelDetectionTask newTask =
        ParcelDetectionTask.builder()
            .statusHistory(
                List.of(TaskStatus.builder().progression(FINISHED).health(SUCCEEDED).build()))
            .build();

    subject.accept(new DetectionTaskStatusChanged(oldTask, newTask));

    var eventCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock, times(1)).accept(eventCaptor.capture());
    ParcelDetectionTaskSucceeded first =
        (ParcelDetectionTaskSucceeded) eventCaptor.getValue().getFirst();
    assertEquals(newTask, first.getTask());
  }

  @Test
  void finished_failed_task_ok() {
    ParcelDetectionTask oldTask =
        ParcelDetectionTask.builder()
            .statusHistory(
                List.of(TaskStatus.builder().progression(PROCESSING).health(UNKNOWN).build()))
            .build();
    ParcelDetectionTask newTask =
        ParcelDetectionTask.builder()
            .statusHistory(
                List.of(TaskStatus.builder().progression(FINISHED).health(FAILED).build()))
            .build();

    subject.accept(new DetectionTaskStatusChanged(oldTask, newTask));

    var eventCaptor = ArgumentCaptor.forClass(ParcelDetectionTask.class);
    verify(taskStatusServiceMock, times(1)).fail(eventCaptor.capture());
    ParcelDetectionTask first = eventCaptor.getValue();
    assertEquals(newTask, first);
  }

  @Test
  void finished_unknown_ko() {
    ParcelDetectionTask oldProcessingTask =
        ParcelDetectionTask.builder()
            .statusHistory(
                List.of(TaskStatus.builder().progression(PROCESSING).health(UNKNOWN).build()))
            .build();
    ParcelDetectionTask oldPendingTask =
        ParcelDetectionTask.builder()
            .statusHistory(
                List.of(TaskStatus.builder().progression(PENDING).health(UNKNOWN).build()))
            .build();
    ParcelDetectionTask newTask =
        ParcelDetectionTask.builder()
            .statusHistory(
                List.of(TaskStatus.builder().progression(FINISHED).health(UNKNOWN).build()))
            .build();

    assertThrows(
        IllegalStateException.class,
        () -> subject.accept(new DetectionTaskStatusChanged(oldProcessingTask, newTask)));
    assertThrows(
        IllegalStateException.class,
        () -> subject.accept(new DetectionTaskStatusChanged(oldPendingTask, newTask)));
  }
}
