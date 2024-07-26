package app.bpartners.geojobs.service.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.gen.annotator.endpoint.rest.model.Job;
import app.bpartners.geojobs.endpoint.event.model.AnnotationTaskRetrievingSubmitted;
import app.bpartners.geojobs.repository.HumanDetectionJobRepository;
import app.bpartners.geojobs.repository.model.detection.HumanDetectionJob;
import app.bpartners.geojobs.service.annotator.AnnotationService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AnnotationTaskRetrievingSubmittedServiceTest {
  private static final String MOCK_JOB_ID = "mock_job_id";
  private static final String MOCK_ANNOTATION_JOB_ID = "mock_first_job_id";

  AnnotationService annotationService = mock();
  HumanDetectionJobRepository humanDetectionJobRepository = mock();
  AnnotationTaskRetrievingSubmittedService subject =
      new AnnotationTaskRetrievingSubmittedService(annotationService, humanDetectionJobRepository);

  Job annotationJob() {
    return new Job().id(MOCK_ANNOTATION_JOB_ID).imagesWidth(1024);
  }

  AnnotationTaskRetrievingSubmitted submitted() {
    return AnnotationTaskRetrievingSubmitted.builder().jobId(MOCK_JOB_ID).build();
  }

  HumanDetectionJob humanDetectionJob() {
    return HumanDetectionJob.builder().annotationJobId(MOCK_ANNOTATION_JOB_ID).build();
  }

  @Test
  void accept_ok() {
    var jobIdsCapture = ArgumentCaptor.forClass(String.class);
    var annotationJobIdsCapture = ArgumentCaptor.forClass(String.class);
    var imageSizesCapture = ArgumentCaptor.forClass(Integer.class);
    when(humanDetectionJobRepository.findByZoneDetectionJobId(MOCK_JOB_ID))
        .thenReturn(List.of(humanDetectionJob()));
    when(annotationService.getAnnotationJobById(MOCK_ANNOTATION_JOB_ID))
        .thenReturn(annotationJob());

    subject.accept(submitted());
    verify(annotationService, times(1))
        .fireTasks(
            jobIdsCapture.capture(),
            annotationJobIdsCapture.capture(),
            imageSizesCapture.capture());

    var jobIdsValues = jobIdsCapture.getAllValues();
    var annotationJobIdsValues = annotationJobIdsCapture.getAllValues();
    var imageSizesValues = imageSizesCapture.getAllValues();

    assertEquals(submitted().getJobId(), jobIdsValues.getFirst());
    assertEquals(MOCK_ANNOTATION_JOB_ID, annotationJobIdsValues.getFirst());
    assertEquals(1024, imageSizesValues.getFirst());
  }
}
