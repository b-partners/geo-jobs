package app.bpartners.geojobs.service.event;

import static app.bpartners.gen.annotator.endpoint.rest.model.JobStatus.COMPLETED;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.AnnotationJobVerificationSent;
import app.bpartners.geojobs.endpoint.event.model.AnnotationTaskRetrievingSubmitted;
import app.bpartners.geojobs.repository.HumanDetectionJobRepository;
import app.bpartners.geojobs.repository.model.AnnotationRetrievingJob;
import app.bpartners.geojobs.repository.model.AnnotationRetrievingTask;
import app.bpartners.geojobs.service.AnnotationRetrievingJobService;
import app.bpartners.geojobs.service.AnnotationRetrievingTaskService;
import app.bpartners.geojobs.service.annotator.AnnotationService;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AnnotationJobVerificationSentService
    implements Consumer<AnnotationJobVerificationSent> {
  private final HumanDetectionJobRepository humanDetectionJobRepository;
  private final AnnotationService annotationService;
  private EventProducer<AnnotationTaskRetrievingSubmitted> eventProducer;
  private final AnnotationRetrievingJobService annotationRetrievingJobService;

  @Override
  public void accept(AnnotationJobVerificationSent annotationJobVerificationSent) {
    var humanZdjId = annotationJobVerificationSent.getHumanZdjId();
    var humanDetectionJobs = humanDetectionJobRepository.findByZoneDetectionJobId(humanZdjId);

    if (humanDetectionJobs.isEmpty()) {
      return;
    }

    var annotationJobs =
        humanDetectionJobs.stream()
            .map(
                humanDetectionJob ->
                    annotationService.getAnnotationJobById(humanDetectionJob.getAnnotationJobId()))
            .toList();
    if (annotationJobs.stream().allMatch(job -> COMPLETED.equals(job.getStatus()))) {
      annotationJobs.forEach(
          job ->{
            var annotationJobId = job.getId();
            var retrievingJob = annotationRetrievingJobService.save(AnnotationRetrievingJob.builder()
                    .id(randomUUID().toString())
                    .annotationJobId(annotationJobId)
                    .detectionJobId(humanZdjId)
                    .statusHistory(List.of())
                .build());
            eventProducer.accept(
                List.of(
                    new AnnotationTaskRetrievingSubmitted(
                        humanZdjId, retrievingJob.getId(), retrievingJob.getAnnotationJobId(), job.getImagesWidth())));
          });
    }
  }
}
