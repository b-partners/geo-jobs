package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.annotation.JobAnnotationProcessed;
import app.bpartners.geojobs.repository.DetectableObjectConfigurationRepository;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.detection.Detection;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class JobAnnotationProcessedService implements Consumer<JobAnnotationProcessed> {
  private final ZoneDetectionJobAnnotationProcessor zoneDetectionJobAnnotationProcessor;
  private final DetectableObjectConfigurationRepository objectConfigurationRepository;
  private final DetectionRepository detectionRepository;

  @Override
  public void accept(JobAnnotationProcessed jobAnnotationProcessed) {
    var jobId = jobAnnotationProcessed.getJobId();
    var annotationJobWithObjectsIdTruePositive =
        jobAnnotationProcessed.getAnnotationJobWithObjectsIdTruePositive();
    var annotationJobWithObjectsIdFalsePositive =
        jobAnnotationProcessed.getAnnotationJobWithObjectsIdFalsePositive();
    var annotationJobWithoutObjectsId = jobAnnotationProcessed.getAnnotationJobWithoutObjectsId();
    var minConfidence = jobAnnotationProcessed.getMinConfidence();
    var persistedObjectConfigurations =
        objectConfigurationRepository.findAllByDetectionJobId(jobId);
    var optionalDetection = detectionRepository.findByZdjId(jobId);
    var detectableObjectConfigurations =
        persistedObjectConfigurations.isEmpty()
            ? (optionalDetection
                .map(Detection::getDetectableObjectConfigurations)
                .orElseGet(List::of))
            : persistedObjectConfigurations;

    zoneDetectionJobAnnotationProcessor.accept(
        jobId,
        minConfidence,
        annotationJobWithObjectsIdTruePositive,
        annotationJobWithObjectsIdFalsePositive,
        annotationJobWithoutObjectsId,
        detectableObjectConfigurations);
  }
}
