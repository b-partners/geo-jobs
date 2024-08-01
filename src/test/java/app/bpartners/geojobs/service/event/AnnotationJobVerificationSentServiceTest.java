package app.bpartners.geojobs.service.event;

import static app.bpartners.gen.annotator.endpoint.rest.model.JobStatus.COMPLETED;
import static app.bpartners.gen.annotator.endpoint.rest.model.JobStatus.PENDING;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.gen.annotator.endpoint.rest.model.Job;
import app.bpartners.gen.annotator.endpoint.rest.model.JobStatus;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.AnnotationJobVerificationSent;
import app.bpartners.geojobs.endpoint.event.model.AnnotationTaskRetrievingSubmitted;
import app.bpartners.geojobs.repository.HumanDetectionJobRepository;
import app.bpartners.geojobs.repository.model.AnnotationRetrievingJob;
import app.bpartners.geojobs.repository.model.detection.HumanDetectionJob;
import app.bpartners.geojobs.service.AnnotationRetrievingJobService;
import app.bpartners.geojobs.service.annotator.AnnotationService;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnnotationJobVerificationSentServiceTest {
  HumanDetectionJobRepository humanDetectionJobRepository = mock();
  AnnotationService annotationService = mock();
  EventProducer<AnnotationTaskRetrievingSubmitted> eventProducer = mock();
  AnnotationRetrievingJobService annotationRetrievingJobService = mock();
  AnnotationJobVerificationSentService subject =
      new AnnotationJobVerificationSentService(
          humanDetectionJobRepository,
          annotationService,
          eventProducer,
          annotationRetrievingJobService);

  @Test
  void should_accept_do_nothing_if_human_detection_job_is_empty() {
    when(humanDetectionJobRepository.findByZoneDetectionJobId(any())).thenReturn(List.of());
    AnnotationJobVerificationSent annotationJobVerificationSent = mock();

    subject.accept(annotationJobVerificationSent);

    verify(eventProducer, never()).accept(any());
    verify(annotationService, never()).getAnnotationJobById(any());
  }

  @Test
  void should_not_produce_event_if_task_status_are_not_all_completed() {
    AnnotationJobVerificationSent annotationJobVerificationSent = mock();
    var completedHumanDetectionJob = asHumanDetectionJob("finishedJobId");
    var notCompletedHumanDetectionJob = asHumanDetectionJob("notCompletedId");
    var completedJob = asJob(COMPLETED);
    var pendingJob = asJob(PENDING);
    var humanDetectionJobs = List.of(completedHumanDetectionJob, notCompletedHumanDetectionJob);

    when(annotationService.getAnnotationJobById(completedHumanDetectionJob.getAnnotationJobId()))
        .thenReturn(completedJob);
    when(annotationService.getAnnotationJobById(notCompletedHumanDetectionJob.getAnnotationJobId()))
        .thenReturn(pendingJob);
    when(humanDetectionJobRepository.findByZoneDetectionJobId(any()))
        .thenReturn(humanDetectionJobs);
    subject.accept(annotationJobVerificationSent);

    verify(eventProducer, never()).accept(any());
  }

  @Test
  void should_produce_event_all_human_detection_job_are_completed() {
    var completedHumanDetectionJobOne = asHumanDetectionJob("dummyId1");
    var completedHumanDetectionJobTwo = asHumanDetectionJob("dummyId2");
    var completedJob = asJob(COMPLETED);
    AnnotationJobVerificationSent annotationJobVerificationSent = mock();
    List<HumanDetectionJob> humanDetectionJobs =
        List.of(completedHumanDetectionJobOne, completedHumanDetectionJobTwo);
    var annotationJobVerificationSendId = "dummyId";
    var annotationRetrievingJobId = "retrievingJobId";

    var exceptedAnnotationTaskRetrievingSubmitted =
        new AnnotationTaskRetrievingSubmitted(
            annotationJobVerificationSendId,
            annotationRetrievingJobId,
            completedJob.getId(),
            completedJob.getImagesWidth());

    when(annotationJobVerificationSent.getHumanZdjId()).thenReturn(annotationJobVerificationSendId);
    when(annotationService.getAnnotationJobById(any())).thenReturn(completedJob);
    when(humanDetectionJobRepository.findByZoneDetectionJobId(any()))
        .thenReturn(humanDetectionJobs);
    when(annotationRetrievingJobService.save(any()))
        .thenReturn(
            AnnotationRetrievingJob.builder()
                .id(annotationRetrievingJobId)
                .annotationJobId(completedJob.getId())
                .build());
    subject.accept(annotationJobVerificationSent);

    verify(eventProducer, times(humanDetectionJobs.size()))
        .accept(List.of(exceptedAnnotationTaskRetrievingSubmitted));
  }

  HumanDetectionJob asHumanDetectionJob(String id) {
    return HumanDetectionJob.builder().id(id).build();
  }

  Job asJob(JobStatus status) {
    return new Job().status(status).id("dummyId").imagesWidth(2_000);
  }
}
