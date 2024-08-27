package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.annotation.JobAnnotationProcessed;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.DetectableObjectConfigurationMapper;
import app.bpartners.geojobs.repository.DetectableObjectConfigurationRepository;
import app.bpartners.geojobs.repository.FullDetectionRepository;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class JobAnnotationProcessedService implements Consumer<JobAnnotationProcessed> {
  private final ZoneDetectionJobAnnotationProcessor zoneDetectionJobAnnotationProcessor;
  private final DetectableObjectConfigurationRepository objectConfigurationRepository;
  private final FullDetectionRepository fullDetectionRepository;
  private final DetectableObjectConfigurationMapper objectConfigurationMapper;

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
    var optionalFullDetection = fullDetectionRepository.findByZdjId(jobId);
    var detectableObjectConfigurations =
        persistedObjectConfigurations.isEmpty()
            ? (optionalFullDetection
                .map(
                    fullDetection ->
                        List.of(
                            objectConfigurationMapper.toDomain(
                                jobId, fullDetection.getDetectableObjectConfiguration())))
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
