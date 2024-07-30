package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.AnnotationTaskRetrievingSubmitted;
import app.bpartners.geojobs.repository.HumanDetectionJobRepository;
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
  private final HumanDetectionJobRepository humanDetectionJobRepository;

  @Override
  public void accept(AnnotationTaskRetrievingSubmitted submitted) {
    var jobId = submitted.getZdjId();
    var humanZdjId = submitted.getHumanZdjId();
    var humanDetectionJob = humanDetectionJobRepository.findByZoneDetectionJobId(humanZdjId);
    log.info("Human detection job count {}", humanDetectionJob.size());
    humanDetectionJob.forEach(
        humanJob -> {
          var annotationJob = annotationService.getAnnotationJobById(humanJob.getAnnotationJobId());
          log.info("Annotation Job {}", annotationJob.getId());
          annotationService.fireTasks(jobId, annotationJob.getId(), annotationJob.getImagesWidth());
        });
  }
}
