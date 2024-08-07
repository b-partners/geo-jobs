package app.bpartners.geojobs.service.event;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationTaskRetrievingSubmitted;
import app.bpartners.geojobs.repository.model.AnnotationRetrievingJob;
import app.bpartners.geojobs.repository.model.detection.HumanDetectionJob;
import app.bpartners.geojobs.service.AnnotationRetrievingJobService;
import app.bpartners.geojobs.service.annotator.AnnotationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AnnotationTaskRetrievingSubmittedServiceTest {
  private static final String MOCK_HUMAN_JOB_ID = "mock_job_id";
  private static final String MOCK_ANNOTATION_JOB_ID = "mock_annotation_job_id";

  AnnotationService annotationService = mock();
  AnnotationRetrievingJobService annotationRetrievingJobService = mock();
  AnnotationTaskRetrievingSubmittedService subject =
      new AnnotationTaskRetrievingSubmittedService(
          annotationService, annotationRetrievingJobService);

  AnnotationTaskRetrievingSubmitted submitted() {
    return AnnotationTaskRetrievingSubmitted.builder()
        .humanZdjId(MOCK_HUMAN_JOB_ID)
        .annotationJobId(MOCK_ANNOTATION_JOB_ID)
        .imageWidth(1024)
        .build();
  }

  HumanDetectionJob humanDetectionJob() {
    return HumanDetectionJob.builder().annotationJobId(MOCK_ANNOTATION_JOB_ID).build();
  }

  @Test
  void accept_ok() {
    var jobIdsCapture = ArgumentCaptor.forClass(String.class);
    var annotationJobIdsCapture = ArgumentCaptor.forClass(String.class);
    var imageSizesCapture = ArgumentCaptor.forClass(Integer.class);
    when(annotationRetrievingJobService.getById(any()))
        .thenReturn(
            AnnotationRetrievingJob.builder()
                .id(randomUUID().toString())
                .annotationJobId(MOCK_ANNOTATION_JOB_ID)
                .build());

    subject.accept(submitted());
    verify(annotationService, times(1))
        .fireTasks(
            jobIdsCapture.capture(),
            any(),
            annotationJobIdsCapture.capture(),
            imageSizesCapture.capture());

    var jobIdsValues = jobIdsCapture.getAllValues();
    var annotationJobIdsValues = annotationJobIdsCapture.getAllValues();
    var imageSizesValues = imageSizesCapture.getAllValues();

    assertEquals(submitted().getHumanZdjId(), jobIdsValues.getFirst());
    assertEquals(MOCK_ANNOTATION_JOB_ID, annotationJobIdsValues.getFirst());
    assertEquals(1024, imageSizesValues.getFirst());
  }
}
