package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.AnnotationRetrievingJobStatusRecomputingSubmitted;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.repository.model.AnnotationRetrievingTask;
import app.bpartners.geojobs.service.AnnotationRetrievingJobService;
import app.bpartners.geojobs.service.AnnotationRetrievingTaskService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.FAILED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static java.util.UUID.randomUUID;

@Service
@AllArgsConstructor
public class AnnotationRetrievingJobStatusRecomputingSubmittedService implements Consumer<AnnotationRetrievingJobStatusRecomputingSubmitted> {
  private final AnnotationRetrievingTaskService annotationRetrievingTaskService;
  private final AnnotationRetrievingJobService annotationRetrievingJobService;

  @Override
  public void accept(AnnotationRetrievingJobStatusRecomputingSubmitted submitted) {
    var linkedJobId = submitted.getAnnotationRetrievingJobId();
    var retrievingTasks = annotationRetrievingTaskService.getByRetrievingJobId(linkedJobId);
    if (retrievingTasks.isEmpty()) {
      return;
    }
    if (retrievingTasks.stream().allMatch(AnnotationRetrievingTask::isSucceeded)) {
      var retrievingJob = annotationRetrievingJobService.getByAnnotationJobId(linkedJobId);
      retrievingJob.hasNewStatus(newStatus(SUCCEEDED));
    }
    if (retrievingTasks.stream().anyMatch(AnnotationRetrievingTask::isFailed)) {
      var retrievingJob = annotationRetrievingJobService.getByAnnotationJobId(linkedJobId);
      retrievingJob.hasNewStatus(newStatus(FAILED));
    }
  }

  private Status newStatus(Status.HealthStatus health) {
    return Status.builder()
        .id(randomUUID().toString())
        .progression(FINISHED)
        .health(health)
        .build();
  }
}
