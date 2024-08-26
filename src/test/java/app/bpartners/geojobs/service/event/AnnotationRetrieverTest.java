package app.bpartners.geojobs.service.event;

import static app.bpartners.gen.annotator.endpoint.rest.model.JobStatus.COMPLETED;
import static app.bpartners.gen.annotator.endpoint.rest.model.JobStatus.PENDING;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;

import app.bpartners.gen.annotator.endpoint.rest.model.Job;
import app.bpartners.gen.annotator.endpoint.rest.model.JobStatus;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationJobVerificationSent;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationTaskRetrievingSubmitted;
import app.bpartners.geojobs.repository.HumanDetectionJobRepository;
import app.bpartners.geojobs.repository.model.AnnotationRetrievingJob;
import app.bpartners.geojobs.repository.model.detection.HumanDetectionJob;
import app.bpartners.geojobs.service.AnnotationRetrievingJobService;
import app.bpartners.geojobs.service.annotator.AnnotationService;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("TODO: update tests as new AnnotationRetriever new implementation")
class AnnotationRetrieverTest {
  private static final String DETECTION_JOB_ID = "detectionJobId";
  HumanDetectionJobRepository humanDetectionJobRepositoryMock = mock();
  AnnotationService annotationServiceMock = mock();
  AnnotationRetrievingJobService annotationRetrievingJobServiceMock = mock();
  AnnotationRetriever subject =
      new AnnotationRetriever(
          humanDetectionJobRepositoryMock,
          annotationServiceMock,
          annotationRetrievingJobServiceMock);

  @Test
  void should_accept_do_nothing_if_human_detection_job_is_empty() {
    when(humanDetectionJobRepositoryMock.findAllByZoneDetectionJobId(any())).thenReturn(List.of());

    subject.accept(DETECTION_JOB_ID);

    verify(annotationServiceMock, never()).getAnnotationJobById(any());
  }

  @Test
  void should_not_produce_event_if_task_status_are_not_all_completed() {
    var completedHumanDetectionJob = asHumanDetectionJob("finishedJobId");
    var notCompletedHumanDetectionJob = asHumanDetectionJob("notCompletedId");
    var completedJob = asJob(COMPLETED);
    var pendingJob = asJob(PENDING);
    var humanDetectionJobs = List.of(completedHumanDetectionJob, notCompletedHumanDetectionJob);
    when(annotationServiceMock.getAnnotationJobById(
            completedHumanDetectionJob.getAnnotationJobId()))
        .thenReturn(completedJob);
    when(annotationServiceMock.getAnnotationJobById(
            notCompletedHumanDetectionJob.getAnnotationJobId()))
        .thenReturn(pendingJob);
    when(humanDetectionJobRepositoryMock.findAllByZoneDetectionJobId(any()))
        .thenReturn(humanDetectionJobs);

    assertDoesNotThrow(() -> subject.accept(DETECTION_JOB_ID));
  }

  @Test
  void should_produce_event_all_human_detection_job_are_completed() {
    var completedHumanDetectionJobOne = asHumanDetectionJob("dummyId1");
    var completedHumanDetectionJobTwo = asHumanDetectionJob("dummyId2");
    var completedJob = asJob(COMPLETED);
    AnnotationJobVerificationSent annotationJobVerificationSent = mock();
    List<HumanDetectionJob> humanDetectionJobs =
        List.of(completedHumanDetectionJobOne, completedHumanDetectionJobTwo);
    var annotationJobVerificationSendId = DETECTION_JOB_ID;
    var annotationRetrievingJobId = "retrievingJobId";

    var exceptedAnnotationTaskRetrievingSubmitted =
        new AnnotationTaskRetrievingSubmitted(
            annotationJobVerificationSendId,
            annotationRetrievingJobId,
            completedJob.getId(),
            completedJob.getImagesWidth());
    when(annotationJobVerificationSent.getHumanZdjId()).thenReturn(annotationJobVerificationSendId);
    when(annotationServiceMock.getAnnotationJobById(any())).thenReturn(completedJob);
    when(humanDetectionJobRepositoryMock.findAllByZoneDetectionJobId(any()))
        .thenReturn(humanDetectionJobs);
    when(annotationRetrievingJobServiceMock.saveAll(any()))
        .thenReturn(
            List.of(
                AnnotationRetrievingJob.builder()
                    .id(annotationRetrievingJobId)
                    .annotationJobId(completedJob.getId())
                    .build()));

    assertDoesNotThrow(() -> subject.accept(DETECTION_JOB_ID));
  }

  HumanDetectionJob asHumanDetectionJob(String id) {
    return HumanDetectionJob.builder().id(id).build();
  }

  Job asJob(JobStatus status) {
    return new Job().status(status).id(DETECTION_JOB_ID).imagesWidth(1024);
  }
}
