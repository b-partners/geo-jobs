package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.annotation.JobAnnotationProcessed;
import app.bpartners.geojobs.repository.DetectableObjectConfigurationRepository;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class JobAnnotationProcessedService implements Consumer<JobAnnotationProcessed> {
  private final ZoneDetectionJobAnnotationProcessor zoneDetectionJobAnnotationProcessor;
  private final DetectableObjectConfigurationRepository objectConfigurationRepository;

  @Override
  public void accept(JobAnnotationProcessed jobAnnotationProcessed) {
    var jobId = jobAnnotationProcessed.getJobId();
    var annotationJobWithObjectsIdTruePositive =
        jobAnnotationProcessed.getAnnotationJobWithObjectsIdTruePositive();
    var annotationJobWithObjectsIdFalsePositive =
        jobAnnotationProcessed.getAnnotationJobWithObjectsIdFalsePositive();
    var annotationJobWithoutObjectsId = jobAnnotationProcessed.getAnnotationJobWithoutObjectsId();
    var minConfidence = jobAnnotationProcessed.getMinConfidence();
    List<DetectableObjectConfiguration> detectableObjectConfigurations =
        objectConfigurationRepository.findAllByDetectionJobId(jobId);

    zoneDetectionJobAnnotationProcessor.accept(
        jobId,
        minConfidence,
        annotationJobWithObjectsIdTruePositive,
        annotationJobWithObjectsIdFalsePositive,
        annotationJobWithoutObjectsId,
        detectableObjectConfigurations);
  }
}
