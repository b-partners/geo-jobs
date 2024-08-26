package app.bpartners.geojobs.service.event;

import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.DetectableObjectConfigurationMapper;
import app.bpartners.geojobs.repository.DetectableObjectConfigurationRepository;
import app.bpartners.geojobs.repository.FullDetectionRepository;
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
  private final FullDetectionRepository fullDetectionRepository;
  private final DetectableObjectConfigurationRepository objectConfigurationRepository;
  private final DetectableObjectConfigurationMapper detectableObjectConfigurationMapper;

  // TODO: once superclass created, consume it
  public void accept(String jobId) {
    var annotationJobWithObjectsIdTruePositive = randomUUID().toString();
    var annotationJobWithObjectsIdFalsePositive = randomUUID().toString();
    var annotationJobWithoutObjectsId = randomUUID().toString();
    var optionalFullDetection = fullDetectionRepository.findByZdjId(jobId);
    var confidence = DEFAULT_MIN_CONFIDENCE;
    List<DetectableObjectConfiguration> detectableObjectConfigurations =
        objectConfigurationRepository.findAllByDetectionJobId(jobId);
    if (optionalFullDetection.isPresent()) {
      var fullDetection = optionalFullDetection.get();
      DetectableObjectConfiguration detectableObjectConfiguration =
          detectableObjectConfigurationMapper.toDomain(
              jobId, fullDetection.getDetectableObjectConfiguration());
      detectableObjectConfigurations = List.of(detectableObjectConfiguration);
      confidence = detectableObjectConfiguration.getConfidence();
    }

    jobAnnotationProcessor.accept(
        jobId,
        confidence,
        annotationJobWithObjectsIdTruePositive,
        annotationJobWithObjectsIdFalsePositive,
        annotationJobWithoutObjectsId,
        detectableObjectConfigurations);
  }
}
