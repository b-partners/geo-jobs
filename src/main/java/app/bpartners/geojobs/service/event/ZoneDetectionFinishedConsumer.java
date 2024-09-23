package app.bpartners.geojobs.service.event;

import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.repository.DetectableObjectConfigurationRepository;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

// TODO: add superclass for ZDJ succeeded and failed
@Component
@AllArgsConstructor
public class ZoneDetectionFinishedConsumer {
  public static final double DEFAULT_MIN_CONFIDENCE = 0.95;
  private final ZoneDetectionJobAnnotationProcessor jobAnnotationProcessor;
  private final DetectionRepository detectionRepository;
  private final DetectableObjectConfigurationRepository objectConfigurationRepository;

  // TODO: once superclass created, consume it
  public void accept(String jobId) {
    var annotationJobWithObjectsIdTruePositive = randomUUID().toString();
    var annotationJobWithObjectsIdFalsePositive = randomUUID().toString();
    var annotationJobWithoutObjectsId = randomUUID().toString();
    var optionalFullDetection = detectionRepository.findByZdjId(jobId);
    List<DetectableObjectConfiguration> detectableObjectConfigurations =
        objectConfigurationRepository.findAllByDetectionJobId(jobId);
    if (optionalFullDetection.isPresent()) {
      var fullDetection = optionalFullDetection.get();
      detectableObjectConfigurations = fullDetection.getDetectableObjectConfigurations();
    }

    jobAnnotationProcessor.accept(
        jobId,
        DEFAULT_MIN_CONFIDENCE,
        annotationJobWithObjectsIdTruePositive,
        annotationJobWithObjectsIdFalsePositive,
        annotationJobWithoutObjectsId,
        detectableObjectConfigurations);
  }
}
