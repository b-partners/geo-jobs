package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.FAILED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationRetrievingJobStatusRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.status.ZDJStatusRecomputingSubmitted;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.repository.model.AnnotationRetrievingTask;
import app.bpartners.geojobs.service.AnnotationRetrievingJobService;
import app.bpartners.geojobs.service.AnnotationRetrievingTaskService;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AnnotationRetrievingJobStatusRecomputingSubmittedService
    implements Consumer<AnnotationRetrievingJobStatusRecomputingSubmitted> {
  private final AnnotationRetrievingTaskService annotationRetrievingTaskService;
  private final AnnotationRetrievingJobService annotationRetrievingJobService;
  private final EventProducer<ZDJStatusRecomputingSubmitted> eventProducer;

  @Override
  public void accept(AnnotationRetrievingJobStatusRecomputingSubmitted submitted) {
    var linkedJobId = submitted.getAnnotationRetrievingJobId();
    var retrievingTasks = annotationRetrievingTaskService.getByRetrievingJobId(linkedJobId);
    if (retrievingTasks.isEmpty()) {
      return;
    }
    var retrievingJob = annotationRetrievingJobService.getByAnnotationJobId(linkedJobId);
    if (retrievingTasks.stream().allMatch(AnnotationRetrievingTask::isSucceeded)) {
      retrievingJob.hasNewStatus(newStatus(SUCCEEDED));
      eventProducer.accept(
          List.of(new ZDJStatusRecomputingSubmitted(retrievingJob.getDetectionJobId())));
      return;
    }
    if (retrievingTasks.stream().anyMatch(AnnotationRetrievingTask::isFailed)) {
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