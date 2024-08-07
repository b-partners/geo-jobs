package app.bpartners.geojobs.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.parcel.ParcelDetectionTaskFailed;
import app.bpartners.geojobs.job.service.TaskStatusService;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionTask;
import app.bpartners.geojobs.service.event.ExceptionToStringFunction;
import app.bpartners.geojobs.service.event.ParcelDetectionTaskConsumer;
import app.bpartners.geojobs.service.event.ParcelDetectionTaskFailedService;
import org.junit.jupiter.api.Test;

public class ParcelDetectionTaskFailedServiceTest {
  ExceptionToStringFunction exceptionToStringFunction = new ExceptionToStringFunction();
  TaskStatusService<ParcelDetectionTask> taskStatusServiceMock = mock();
  ParcelDetectionTaskConsumer detectionTaskConsumerMock = mock();
  EventProducer eventProducerMock = mock();
  ParcelDetectionTaskFailedService subject =
      new ParcelDetectionTaskFailedService(
          taskStatusServiceMock,
          detectionTaskConsumerMock,
          eventProducerMock,
          exceptionToStringFunction);

  @Test
  void task_consumed_ok() {
    ParcelDetectionTask parcelDetectionTask = ParcelDetectionTask.builder().build();

    assertDoesNotThrow(() -> subject.accept(new ParcelDetectionTaskFailed(parcelDetectionTask, 1)));
    verify(taskStatusServiceMock, times(0)).fail(parcelDetectionTask);
    verify(detectionTaskConsumerMock, times(1)).accept(parcelDetectionTask);
    verify(eventProducerMock, times(0)).accept(any());
  }

  @Test
  void task_not_consumed_ok() {
    ParcelDetectionTask parcelDetectionTask = ParcelDetectionTask.builder().build();
    doThrow(RuntimeException.class).when(detectionTaskConsumerMock).accept(parcelDetectionTask);

    assertDoesNotThrow(() -> subject.accept(new ParcelDetectionTaskFailed(parcelDetectionTask, 1)));
    verify(taskStatusServiceMock, times(0)).fail(parcelDetectionTask);
    verify(detectionTaskConsumerMock, times(1)).accept(parcelDetectionTask);
    verify(eventProducerMock, times(1)).accept(any());
  }

  @Test
  void max_attempt_nb_reached_ko() {
    ParcelDetectionTask parcelDetectionTask = ParcelDetectionTask.builder().build();

    assertDoesNotThrow(() -> subject.accept(new ParcelDetectionTaskFailed(parcelDetectionTask, 4)));

    verify(taskStatusServiceMock, times(1)).fail(parcelDetectionTask);
    verify(detectionTaskConsumerMock, times(0)).accept(parcelDetectionTask);
    verify(eventProducerMock, times(0)).accept(any());
  }
}
