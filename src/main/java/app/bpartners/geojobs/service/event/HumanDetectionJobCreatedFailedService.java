package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.gen.HumanDetectionJobCreatedFailed;
import app.bpartners.geojobs.service.annotator.AnnotationService;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class HumanDetectionJobCreatedFailedService
    implements Consumer<HumanDetectionJobCreatedFailed> {
  private final AnnotationService annotationService;
  private final EventProducer eventProducer;
  private static final int MAX_ATTEMPT = 50;

  @Override
  public void accept(HumanDetectionJobCreatedFailed event) {
    var humanZdj = event.getHumanDetectionJob();
    var attemptNb = event.getAttemptNb();
    if (attemptNb > MAX_ATTEMPT) {
      log.error("Max attempt reached for humanDetectionJobFailed={}", humanZdj);
      return;
    }
    try {
      annotationService.sendAnnotationsFromHumanZDJ(humanZdj);
    } catch (Exception e) {
      log.error(
          "Processing humanDetectionJob(id={}) failed with attemptNb = {} and exception message ="
              + " {}",
          humanZdj.getId(),
          attemptNb,
          e.getMessage());
      eventProducer.accept(
          List.of(
              HumanDetectionJobCreatedFailed.builder()
                  .humanDetectionJob(humanZdj)
                  .attemptNb(attemptNb + 1)
                  .build()));
    }
  }
}
