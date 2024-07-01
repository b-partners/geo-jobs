package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.HumanDetectionJobCreatedFailed;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.repository.model.detection.HumanDetectionJob;
import app.bpartners.geojobs.service.annotator.AnnotationService;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class HumanDetectionJobCreatedFailedServiceTest {
  AnnotationService annotationServiceMock = mock();
  EventProducer eventProducerMock = mock();
  ZoneDetectionJobService detectionJobServiceMock = mock();
  ExceptionToStringFunction exceptionToStringFunction = new ExceptionToStringFunction();
  HumanDetectionJobCreatedFailedService subject =
      new HumanDetectionJobCreatedFailedService(
          annotationServiceMock,
          eventProducerMock,
          detectionJobServiceMock,
          exceptionToStringFunction);

  @Test
  @SneakyThrows
  void accept_ok() {
    String humanDetectionJobId = "humanDetectionJobId";
    String annotationJobCustomName = "annotationJobCustomName";
    String exceptionMessage = "Exception message";
    HumanDetectionJob humanDetectionJob = HumanDetectionJob.builder().build();
    int attemptNb = 1;
    when(detectionJobServiceMock.getHumanDetectionJobById(humanDetectionJobId))
        .thenReturn(humanDetectionJob);

    subject.accept(
        HumanDetectionJobCreatedFailed.builder()
            .humanDetectionJobId(humanDetectionJobId)
            .annotationJobCustomName(annotationJobCustomName)
            .attemptNb(attemptNb)
            .exceptionMessage(exceptionMessage)
            .build());

    verify(annotationServiceMock, times(1))
        .createAnnotationJob(humanDetectionJob, annotationJobCustomName);
    verify(eventProducerMock, times(0)).accept(any());
  }

  @Test
  @SneakyThrows
  void accept_retry_ok() {
    String humanDetectionJobId = "humanDetectionJobId";
    String annotationJobCustomName = "annotationJobCustomName";
    String exceptionMessage = "Exception message";
    HumanDetectionJob humanDetectionJob = HumanDetectionJob.builder().build();
    int attemptNb = 1;
    when(detectionJobServiceMock.getHumanDetectionJobById(humanDetectionJobId))
        .thenReturn(humanDetectionJob);
    ApiException expectedException = new ApiException(SERVER_EXCEPTION, "exceptionMessage");
    doThrow(expectedException)
        .when(annotationServiceMock)
        .createAnnotationJob(humanDetectionJob, annotationJobCustomName);

    subject.accept(
        HumanDetectionJobCreatedFailed.builder()
            .humanDetectionJobId(humanDetectionJobId)
            .annotationJobCustomName(annotationJobCustomName)
            .attemptNb(attemptNb)
            .exceptionMessage(exceptionMessage)
            .build());

    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(annotationServiceMock, times(1))
        .createAnnotationJob(humanDetectionJob, annotationJobCustomName);
    verify(eventProducerMock, times(1)).accept(listCaptor.capture());
    var event = ((List<HumanDetectionJobCreatedFailed>) listCaptor.getValue()).getFirst();
    assertEquals(
        HumanDetectionJobCreatedFailed.builder()
            .humanDetectionJobId(humanDetectionJobId)
            .annotationJobCustomName(annotationJobCustomName)
            .exceptionMessage(exceptionToStringFunction.apply(expectedException))
            .attemptNb(attemptNb + 1)
            .build(),
        event);
  }

  @Test
  @SneakyThrows
  void accept_do_nothing_ok() {
    String humanDetectionJobId = "humanDetectionJobId";
    String annotationJobCustomName = "annotationJobCustomName";
    String exceptionMessage = null;
    HumanDetectionJob humanDetectionJob = HumanDetectionJob.builder().build();
    int attemptNb = 4;
    when(detectionJobServiceMock.getHumanDetectionJobById(humanDetectionJobId))
        .thenReturn(humanDetectionJob);

    assertDoesNotThrow(
        () ->
            subject.accept(
                HumanDetectionJobCreatedFailed.builder()
                    .humanDetectionJobId(humanDetectionJobId)
                    .annotationJobCustomName(annotationJobCustomName)
                    .attemptNb(attemptNb)
                    .exceptionMessage(exceptionMessage)
                    .build()));

    verify(annotationServiceMock, times(0)).createAnnotationJob(any());
    verify(eventProducerMock, times(0)).accept(any());
  }
}
