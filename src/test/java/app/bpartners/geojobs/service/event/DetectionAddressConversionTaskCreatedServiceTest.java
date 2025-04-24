package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.FAILED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.service.DetectionAddressConversionTaskConsumer.withNewStatus;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.DetectionAddressConversionTaskCreated;
import app.bpartners.geojobs.endpoint.event.model.DetectionAddressConversionTaskFailed;
import app.bpartners.geojobs.endpoint.event.model.DetectionAddressConversionTaskSucceeded;
import app.bpartners.geojobs.repository.model.DetectionAddressConversionTask;
import app.bpartners.geojobs.service.DetectionAddressConversionTaskConsumer;
import app.bpartners.geojobs.service.DetectionAddressConversionTaskStatusService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.client.HttpServerErrorException;

class DetectionAddressConversionTaskCreatedServiceTest {
  DetectionAddressConversionTaskConsumer taskConsumerMock = mock();
  DetectionAddressConversionTaskStatusService taskStatusServiceMock = mock();
  EventProducer eventProducerMock = mock();
  DetectionAddressConversionTaskCreatedService subject =
      new DetectionAddressConversionTaskCreatedService(
          taskConsumerMock, taskStatusServiceMock, eventProducerMock);

  @Test
  void consumes_task_and_trigger_succeeded_event_with_first_attempt() {
    var taskMock = mock(DetectionAddressConversionTask.class);
    var e2ApiKey = randomUUID().toString();
    doNothing().when(taskConsumerMock).accept(taskMock, e2ApiKey);

    subject.accept(
        DetectionAddressConversionTaskCreated.builder()
            .attemptNb(1)
            .e2ApiKey(e2ApiKey)
            .task(taskMock)
            .build());

    var eventListCaptor = ArgumentCaptor.forClass(List.class);
    verify(taskStatusServiceMock, only()).process(taskMock);
    verify(taskConsumerMock, only()).accept(taskMock, e2ApiKey);
    verify(eventProducerMock, only()).accept(eventListCaptor.capture());
    var eventListCaptorValue = eventListCaptor.getValue();
    assertEquals(1, eventListCaptorValue.size());
    var taskSucceededEvent =
        (DetectionAddressConversionTaskSucceeded) eventListCaptorValue.getFirst();
    assertEquals(
        DetectionAddressConversionTaskSucceeded.builder()
            .succeededTask(withNewStatus(taskMock, FINISHED, SUCCEEDED, null))
            .build(),
        taskSucceededEvent);
  }

  @Test
  void consumes_task_and_trigger_succeeded_event_with_attempt_less_than_max_attempts() {
    var taskMock = mock(DetectionAddressConversionTask.class);
    var e2ApiKey = randomUUID().toString();
    doNothing().when(taskConsumerMock).accept(taskMock, e2ApiKey);

    subject.accept(
        DetectionAddressConversionTaskCreated.builder()
            .attemptNb(3)
            .e2ApiKey(e2ApiKey)
            .task(taskMock)
            .build());

    var eventListCaptor = ArgumentCaptor.forClass(List.class);
    verify(taskStatusServiceMock, never()).process(taskMock);
    verify(taskConsumerMock, only()).accept(taskMock, e2ApiKey);
    verify(eventProducerMock, only()).accept(eventListCaptor.capture());
    var eventListCaptorValue = eventListCaptor.getValue();
    assertEquals(1, eventListCaptorValue.size());
    var taskSucceededEvent =
        (DetectionAddressConversionTaskSucceeded) eventListCaptorValue.getFirst();
    assertEquals(
        DetectionAddressConversionTaskSucceeded.builder()
            .succeededTask(withNewStatus(taskMock, FINISHED, SUCCEEDED, null))
            .build(),
        taskSucceededEvent);
  }

  @Test
  void consumes_task_with_exception_trigger_task_created_event_with_incremented_attempt() {
    var taskMock = mock(DetectionAddressConversionTask.class);
    var e2ApiKey = randomUUID().toString();
    var attemptNb = 4;
    doThrow(HttpServerErrorException.InternalServerError.class)
        .when(taskConsumerMock)
        .accept(taskMock, e2ApiKey);

    assertDoesNotThrow(
        () ->
            subject.accept(
                DetectionAddressConversionTaskCreated.builder()
                    .attemptNb(attemptNb)
                    .e2ApiKey(e2ApiKey)
                    .task(taskMock)
                    .build()));

    var eventListCaptor = ArgumentCaptor.forClass(List.class);
    verify(taskStatusServiceMock, never()).process(taskMock);
    verify(taskConsumerMock, only()).accept(taskMock, e2ApiKey);
    verify(eventProducerMock, only()).accept(eventListCaptor.capture());
    var eventListCaptorValue = eventListCaptor.getValue();
    assertEquals(1, eventListCaptorValue.size());
    var taskCreatedEvent = (DetectionAddressConversionTaskCreated) eventListCaptorValue.getFirst();
    assertEquals(
        DetectionAddressConversionTaskCreated.builder()
            .task(taskMock)
            .e2ApiKey(e2ApiKey)
            .attemptNb(attemptNb + 1)
            .build(),
        taskCreatedEvent);
  }

  @Test
  void max_attempt_nb_reached_and_trigger_failed_event() {
    var taskMock = mock(DetectionAddressConversionTask.class);
    var e2ApiKey = randomUUID().toString();
    var attemptNb = 6;
    doNothing().when(taskConsumerMock).accept(taskMock, e2ApiKey);

    subject.accept(
        DetectionAddressConversionTaskCreated.builder()
            .attemptNb(attemptNb)
            .e2ApiKey(e2ApiKey)
            .task(taskMock)
            .build());

    var eventListCaptor = ArgumentCaptor.forClass(List.class);
    verify(taskStatusServiceMock, never()).process(taskMock);
    verify(taskConsumerMock, never()).accept(taskMock, e2ApiKey);
    verify(eventProducerMock, only()).accept(eventListCaptor.capture());
    var eventListCaptorValue = eventListCaptor.getValue();
    assertEquals(1, eventListCaptorValue.size());
    var taskFailedEvent = (DetectionAddressConversionTaskFailed) eventListCaptorValue.getFirst();
    assertEquals(
        DetectionAddressConversionTaskFailed.builder()
            .task(withNewStatus(taskMock, FINISHED, FAILED, null))
            .build(),
        taskFailedEvent);
  }
}
