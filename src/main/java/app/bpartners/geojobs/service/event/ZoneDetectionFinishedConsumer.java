package app.bpartners.geojobs.service.event;

import static java.util.UUID.randomUUID;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

// TODO: add superclass for ZDJ succeeded and failed
@Component
@AllArgsConstructor
public class ZoneDetectionFinishedConsumer {
  public static final double DEFAULT_MIN_CONFIDENCE = 0.95;
  private final ZoneDetectionJobAnnotationProcessor jobAnnotationProcessor;

  // TODO: once superclass created, consume it
  public void accept(String jobId) {
    var annotationJobWithObjectsIdTruePositive = randomUUID().toString();
    var annotationJobWithObjectsIdFalsePositive = randomUUID().toString();
    var annotationJobWithoutObjectsId = randomUUID().toString();

    jobAnnotationProcessor.accept(
        jobId,
        DEFAULT_MIN_CONFIDENCE,
        annotationJobWithObjectsIdTruePositive,
        annotationJobWithObjectsIdFalsePositive,
        annotationJobWithoutObjectsId);
  }
}
