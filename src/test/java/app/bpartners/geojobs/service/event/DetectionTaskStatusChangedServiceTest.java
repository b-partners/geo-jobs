package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.*;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.DetectionTaskStatusChanged;
import app.bpartners.geojobs.endpoint.event.model.DetectionTaskSucceeded;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.job.service.RetryableTaskStatusService;
import app.bpartners.geojobs.repository.model.detection.DetectionTask;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class DetectionTaskStatusChangedServiceTest {
  EventProducer eventProducerMock = mock();
  RetryableTaskStatusService<DetectionTask, ZoneDetectionJob> taskStatusServiceMock = mock();
  DetectionTaskStatusChangedService subject =
      new DetectionTaskStatusChangedService(eventProducerMock, taskStatusServiceMock);

  @Test
  void finished_succeeded_task_ok() {
    DetectionTask oldTask =
        DetectionTask.builder()
            .statusHistory(
                List.of(TaskStatus.builder().progression(PROCESSING).health(UNKNOWN).build()))
            .build();
    DetectionTask newTask =
        DetectionTask.builder()
            .statusHistory(
                List.of(TaskStatus.builder().progression(FINISHED).health(SUCCEEDED).build()))
            .build();

    subject.accept(new DetectionTaskStatusChanged(oldTask, newTask));

    var eventCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock, times(1)).accept(eventCaptor.capture());
    DetectionTaskSucceeded first = (DetectionTaskSucceeded) eventCaptor.getValue().getFirst();
    assertEquals(newTask, first.getTask());
  }

  @Test
  void finished_failed_task_ok() {
    DetectionTask oldTask =
        DetectionTask.builder()
            .statusHistory(
                List.of(TaskStatus.builder().progression(PROCESSING).health(UNKNOWN).build()))
            .build();
    DetectionTask newTask =
        DetectionTask.builder()
            .statusHistory(
                List.of(TaskStatus.builder().progression(FINISHED).health(FAILED).build()))
            .build();

    subject.accept(new DetectionTaskStatusChanged(oldTask, newTask));

    var eventCaptor = ArgumentCaptor.forClass(DetectionTask.class);
    verify(taskStatusServiceMock, times(1)).fail(eventCaptor.capture());
    DetectionTask first = eventCaptor.getValue();
    assertEquals(newTask, first);
  }

  @Test
  void finished_unknown_ko() {
    DetectionTask oldProcessingTask =
        DetectionTask.builder()
            .statusHistory(
                List.of(TaskStatus.builder().progression(PROCESSING).health(UNKNOWN).build()))
            .build();
    DetectionTask oldPendingTask =
        DetectionTask.builder()
            .statusHistory(
                List.of(TaskStatus.builder().progression(PENDING).health(UNKNOWN).build()))
            .build();
    DetectionTask newTask =
        DetectionTask.builder()
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
