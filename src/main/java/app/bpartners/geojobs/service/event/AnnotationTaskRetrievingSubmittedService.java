package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.AnnotationTaskRetrievingSubmitted;
import app.bpartners.geojobs.repository.HumanDetectionJobRepository;
import app.bpartners.geojobs.service.annotator.AnnotationService;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AnnotationTaskRetrievingSubmittedService
    implements Consumer<AnnotationTaskRetrievingSubmitted> {
  private final AnnotationService annotationService;
  private final HumanDetectionJobRepository humanDetectionJobRepository;

  @Override
  public void accept(AnnotationTaskRetrievingSubmitted submitted) {
    var jobId = submitted.getZdjId();
    var humanZdjId = submitted.getHumanZdjId();
    var humanDetectionJob = humanDetectionJobRepository.findByZoneDetectionJobId(humanZdjId);
    humanDetectionJob.forEach(
        humanJob -> {
          var annotationJob = annotationService.getAnnotationJobById(humanJob.getAnnotationJobId());
          annotationService.fireTasks(jobId, annotationJob.getId(), annotationJob.getImagesWidth());
        });
  }
}
