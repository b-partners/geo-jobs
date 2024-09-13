package app.bpartners.geojobs.service.annotator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationDeliveryJobStatusRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationDeliveryTaskSucceeded;
import app.bpartners.geojobs.job.service.TaskStatusService;
import app.bpartners.geojobs.repository.AnnotationDeliveryTaskRepository;
import app.bpartners.geojobs.repository.model.annotation.AnnotationDeliveryTask;
import app.bpartners.geojobs.service.event.annotation.delivery.AnnotationDeliveryTaskSucceededService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AnnotationDeliveryTaskSucceededServiceTest {
  TaskStatusService<AnnotationDeliveryTask> taskStatusServiceMock = mock();
  AnnotationDeliveryTaskRepository taskRepositoryMock = mock();
  EventProducer eventProducerMock = mock();
  AnnotationDeliveryTaskSucceededService subject =
      new AnnotationDeliveryTaskSucceededService(
          taskStatusServiceMock, taskRepositoryMock, eventProducerMock);

  @Test
  void accept_ok() {
    var task = new AnnotationDeliveryTask();

    subject.accept(AnnotationDeliveryTaskSucceeded.builder().deliveryTask(task).build());

    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(taskRepositoryMock, only()).save(task);
    verify(taskStatusServiceMock, only()).succeed(task);
    verify(eventProducerMock, only()).accept(listCaptor.capture());
    assertEquals(
        AnnotationDeliveryJobStatusRecomputingSubmitted.class,
        listCaptor.getValue().getFirst().getClass());
  }
}
