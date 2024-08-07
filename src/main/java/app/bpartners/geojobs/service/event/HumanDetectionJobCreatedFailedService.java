package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.annotation.HumanDetectionJobCreatedFailed;
import app.bpartners.geojobs.service.annotator.AnnotationService;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Slf4j
public class HumanDetectionJobCreatedFailedService
    implements Consumer<HumanDetectionJobCreatedFailed> {
  private final AnnotationService annotationService;
  private final EventProducer eventProducer;
  private final ZoneDetectionJobService zoneDetectionJobService;
  private final ExceptionToStringFunction exceptionToStringFunction;
  private static final int MAX_ATTEMPT = 50;

  @Override
  @Transactional
  public void accept(HumanDetectionJobCreatedFailed event) {
    int attemptNb = event.getAttemptNb();
    if (attemptNb > 3) {
      log.info("[DEBUG] HumanDetectionJobCreatedFailedService attempt {}", attemptNb);
    }
    var humanZdjId = event.getHumanDetectionJobId();
    var humanZdj = zoneDetectionJobService.getHumanDetectionJobById(event.getHumanDetectionJobId());
    var annotationJobCustomName = event.getAnnotationJobCustomName();
    if (attemptNb > MAX_ATTEMPT) {
      log.info("Max attempt {} reached for humanDetectionJobFailed={}", attemptNb, humanZdj);
      return;
    }
    try {
      if (annotationJobCustomName != null) {
        annotationService.createAnnotationJob(humanZdj, annotationJobCustomName);
      } else {
        annotationService.createAnnotationJob(humanZdj);
      }
    } catch (Exception e) {
      log.info(
          "Processing humanDetectionJob(id={}) failed with attemptNb = {} and exception message ="
              + " {}",
          humanZdj.getId(),
          attemptNb,
          e.getMessage());
      eventProducer.accept(
          List.of(
              HumanDetectionJobCreatedFailed.builder()
                  .humanDetectionJobId(humanZdjId)
                  .annotationJobCustomName(annotationJobCustomName)
                  .attemptNb(attemptNb + 1)
                  .exceptionMessage(exceptionToStringFunction.apply(e))
                  .build()));
    }
  }
}
