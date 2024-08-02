package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PROCESSING;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.model.AnnotationTaskRetrievingSubmitted;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.service.AnnotationRetrievingJobService;
import app.bpartners.geojobs.service.annotator.AnnotationService;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class AnnotationTaskRetrievingSubmittedService
    implements Consumer<AnnotationTaskRetrievingSubmitted> {
  private final AnnotationService annotationService;
  private final AnnotationRetrievingJobService annotationRetrievingJobService;

  @Override
  public void accept(AnnotationTaskRetrievingSubmitted submitted) {
    var humanZdjId = submitted.getHumanZdjId();
    var retrievingJob =
        annotationRetrievingJobService.getById(submitted.getAnnotationRetrievingJobId());
    var annotationJobId = submitted.getAnnotationJobId();
    var imageWidth = submitted.getImageWidth();
    annotationService.fireTasks(humanZdjId, retrievingJob.getId(), annotationJobId, imageWidth);
    retrievingJob.hasNewStatus(
        Status.builder()
            .id(randomUUID().toString())
            .creationDatetime(now())
            .progression(PROCESSING)
            .health(UNKNOWN)
            .build());
  }
}
