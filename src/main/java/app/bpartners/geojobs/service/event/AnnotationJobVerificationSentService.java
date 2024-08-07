package app.bpartners.geojobs.service.event;

import static app.bpartners.gen.annotator.endpoint.rest.model.JobStatus.COMPLETED;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationJobVerificationSent;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationTaskRetrievingSubmitted;
import app.bpartners.geojobs.repository.HumanDetectionJobRepository;
import app.bpartners.geojobs.repository.model.AnnotationRetrievingJob;
import app.bpartners.geojobs.service.AnnotationRetrievingJobService;
import app.bpartners.geojobs.service.annotator.AnnotationService;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AnnotationJobVerificationSentService
    implements Consumer<AnnotationJobVerificationSent> {
  private static final int IMAGE_SIZE = 1024;
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
    if (annotationJobs.isEmpty()) {
      return;
    }
    if (annotationJobs.stream().allMatch(job -> COMPLETED.equals(job.getStatus()))) {
      List<AnnotationRetrievingJob> retrievingJobs =
          annotationJobs.stream()
              .map(
                  aJob ->
                      AnnotationRetrievingJob.builder()
                          .id(randomUUID().toString())
                          .annotationJobId(aJob.getId())
                          .detectionJobId(humanZdjId)
                          .statusHistory(List.of())
                          .build())
              .collect(Collectors.toUnmodifiableList());
      var saved = annotationRetrievingJobService.saveAll(retrievingJobs);

      saved.forEach(
          job ->
              eventProducer.accept(
                  List.of(
                      new AnnotationTaskRetrievingSubmitted(
                          humanZdjId, job.getId(), job.getAnnotationJobId(), IMAGE_SIZE))));
    }
  }
}
