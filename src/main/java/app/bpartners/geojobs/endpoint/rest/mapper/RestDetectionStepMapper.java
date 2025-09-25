package app.bpartners.geojobs.endpoint.rest.mapper;

import app.bpartners.geojobs.endpoint.rest.model.DetectionStep;
import app.bpartners.geojobs.endpoint.rest.model.DetectionStepName;
import app.bpartners.geojobs.endpoint.rest.model.Status;
import org.springframework.stereotype.Component;

@Component
public class RestDetectionStepMapper {
  public DetectionStep fromRepository(
      app.bpartners.geojobs.repository.model.detection.DetectionStep step) {
    return new DetectionStep()
        .name(DetectionStepName.fromValue(step.getName().getValue()))
        .status(
            new Status()
                .progression(
                    Status.ProgressionEnum.fromValue(step.getStatus().getProgression().getValue()))
                .health(Status.HealthEnum.fromValue(step.getStatus().getHealth().getValue())));
  }

  public app.bpartners.geojobs.repository.model.detection.DetectionStep toRepository(
      DetectionStep step) {
    return app.bpartners.geojobs.repository.model.detection.DetectionStep.builder()
        .name(
            app.bpartners.geojobs.repository.model.detection.DetectionStepName.fromValue(
                step.getName().getValue()))
        .status(
            app.bpartners.geojobs.repository.model.detection.Status.builder()
                .progression(
                    app.bpartners.geojobs.repository.model.detection.Status.Progression.valueOf(
                        step.getStatus().getProgression().getValue()))
                .health(
                    app.bpartners.geojobs.repository.model.detection.Status.Health.valueOf(
                        step.getStatus().getHealth().getValue()))
                .build())
        .build();
  }
}
