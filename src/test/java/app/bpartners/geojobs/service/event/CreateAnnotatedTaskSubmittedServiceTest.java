package app.bpartners.geojobs.service.event;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import app.bpartners.gen.annotator.endpoint.rest.model.CreateAnnotatedTask;
import app.bpartners.geojobs.endpoint.event.model.annotation.CreateAnnotatedTaskSubmitted;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.service.annotator.AnnotationService;
import org.junit.jupiter.api.Test;

public class CreateAnnotatedTaskSubmittedServiceTest {
  AnnotationService annotationServiceMock = mock();
  CreateAnnotatedTaskSubmittedService subject =
      new CreateAnnotatedTaskSubmittedService(annotationServiceMock);

  @Test
  void accept_ok() {
    String annotationJobId = "annotationJobId";
    CreateAnnotatedTask createAnnotatedTask = new CreateAnnotatedTask();

    assertDoesNotThrow(
        () ->
            subject.accept(
                CreateAnnotatedTaskSubmitted.builder()
                    .annotationJobId(annotationJobId)
                    .createAnnotatedTask(createAnnotatedTask)
                    .build()));

    verify(annotationServiceMock, times(1)).addAnnotationTask(annotationJobId, createAnnotatedTask);
  }

  @Test
  void accept_ko() {
    String annotationJobId = "annotationJobId";
    CreateAnnotatedTask createAnnotatedTask = new CreateAnnotatedTask();
    doThrow(ApiException.class)
        .when(annotationServiceMock)
        .addAnnotationTask(annotationJobId, createAnnotatedTask);

    assertThrows(
        ApiException.class,
        () ->
            subject.accept(
                CreateAnnotatedTaskSubmitted.builder()
                    .annotationJobId(annotationJobId)
                    .createAnnotatedTask(createAnnotatedTask)
                    .build()));
  }
}
