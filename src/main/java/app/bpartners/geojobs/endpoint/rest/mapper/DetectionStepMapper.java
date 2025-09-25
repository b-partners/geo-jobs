package app.bpartners.geojobs.endpoint.rest.mapper;

import app.bpartners.geojobs.endpoint.rest.model.DetectionStep;
import app.bpartners.geojobs.endpoint.rest.model.DetectionStepName;
import app.bpartners.geojobs.endpoint.rest.model.Status;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class DetectionStepMapper {
  public app.bpartners.geojobs.repository.model.detection.DetectionStep toRepository(
      DetectionStep step) {
    return app.bpartners.geojobs.repository.model.detection.DetectionStep.builder()
        .name(
            app.bpartners.geojobs.repository.model.detection.DetectionStepName.fromValue(
                step.getName().getValue()))
        .progression(
            app.bpartners.geojobs.repository.model.detection.Status.Progression.valueOf(
                step.getStatus().getProgression().getValue()))
        .health(
            app.bpartners.geojobs.repository.model.detection.Status.Health.valueOf(
                step.getStatus().getHealth().getValue()))
        .creationDatetime(Instant.now())
        .build();
  }

  public DetectionStep toRest(app.bpartners.geojobs.repository.model.detection.DetectionStep step) {
    return new DetectionStep()
        .name(DetectionStepName.fromValue(step.getName().getValue()))
        .status(
            new app.bpartners.geojobs.endpoint.rest.model.Status()
                .creationDatetime(step.getCreationDatetime())
                .progression(Status.ProgressionEnum.valueOf(step.getProgression().getValue()))
                .health(Status.HealthEnum.valueOf(step.getHealth().getValue())));
  }
}
