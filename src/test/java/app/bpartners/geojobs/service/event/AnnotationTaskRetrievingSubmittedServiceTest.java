package app.bpartners.geojobs.service.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.gen.annotator.endpoint.rest.model.Job;
import app.bpartners.geojobs.endpoint.event.model.AnnotationTaskRetrievingSubmitted;
import app.bpartners.geojobs.service.annotator.AnnotationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AnnotationTaskRetrievingSubmittedServiceTest {
  private static final String MOCK_JOB_ID = "mock_job_id";
  private static final String MOCK_FIRST_JOB_ID = "mock_first_job_id";
  private static final String MOCK_LAST_JOB_ID = "mock_last_job_id";

  AnnotationService annotationService = mock();
  AnnotationTaskRetrievingSubmittedService subject =
      new AnnotationTaskRetrievingSubmittedService(annotationService);

  Job firstAnnotationJob() {
    return new Job().id(MOCK_FIRST_JOB_ID).imagesWidth(1024);
  }

  Job lastAnnotationJob() {
    return new Job().id(MOCK_LAST_JOB_ID).imagesWidth(256);
  }

  AnnotationTaskRetrievingSubmitted submitted() {
    return AnnotationTaskRetrievingSubmitted.builder()
        .jobId(MOCK_JOB_ID)
        .firstAnnotationJobId(MOCK_FIRST_JOB_ID)
        .lastAnnotationJobId(MOCK_LAST_JOB_ID)
        .build();
  }

  @Test
  void accept_ok() {
    var jobIdsCapture = ArgumentCaptor.forClass(String.class);
    var annotationJobIdsCapture = ArgumentCaptor.forClass(String.class);
    var imageSizesCapture = ArgumentCaptor.forClass(Integer.class);
    when(annotationService.getAnnotationJobById(MOCK_FIRST_JOB_ID))
        .thenReturn(firstAnnotationJob());
    when(annotationService.getAnnotationJobById(MOCK_LAST_JOB_ID)).thenReturn(lastAnnotationJob());

    subject.accept(submitted());
    verify(annotationService, times(2))
        .fireTasks(
            jobIdsCapture.capture(),
            annotationJobIdsCapture.capture(),
            imageSizesCapture.capture());

    var jobIdsValues = jobIdsCapture.getAllValues();
    var annotationJobIdsValues = annotationJobIdsCapture.getAllValues();
    var imageSizesValues = imageSizesCapture.getAllValues();

    assertEquals(submitted().getJobId(), jobIdsValues.getFirst());
    assertEquals(submitted().getJobId(), jobIdsValues.getLast());
    assertEquals(MOCK_FIRST_JOB_ID, annotationJobIdsValues.getFirst());
    assertEquals(MOCK_LAST_JOB_ID, annotationJobIdsValues.getLast());
    assertEquals(1024, imageSizesValues.getFirst());
    assertEquals(256, imageSizesValues.getLast());
  }
}
