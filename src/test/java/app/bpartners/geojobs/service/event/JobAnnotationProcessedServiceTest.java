package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.service.event.ZoneDetectionFinishedConsumer.DEFAULT_MIN_CONFIDENCE;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.model.annotation.JobAnnotationProcessed;
import app.bpartners.geojobs.repository.DetectableObjectConfigurationRepository;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.Detection;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class JobAnnotationProcessedServiceTest {
  ZoneDetectionJobAnnotationProcessor jobAnnotationProcessorMock = mock();
  DetectableObjectConfigurationRepository objectConfigurationRepositoryMock = mock();
  DetectionRepository detectionRepositoryMock = mock();
  JobAnnotationProcessedService subject =
      new JobAnnotationProcessedService(
          jobAnnotationProcessorMock, objectConfigurationRepositoryMock, detectionRepositoryMock);

  @Test
  void accept_with_persisted_object_conf_ok() {
    List<DetectableObjectConfiguration> objectConfigurations =
        List.of(new DetectableObjectConfiguration());
    when(objectConfigurationRepositoryMock.findAllByDetectionJobId(any()))
        .thenReturn(objectConfigurations);
    String jobId = "jobId";
    double minConfidence = 0.8;
    String truePositive = "truePositive";
    String falsePositive = "falsePositive";
    String withoutObjects = "withoutObjects";

    subject.accept(
        JobAnnotationProcessed.builder()
            .jobId(jobId)
            .minConfidence(minConfidence)
            .annotationJobWithObjectsIdTruePositive(truePositive)
            .annotationJobWithObjectsIdFalsePositive(falsePositive)
            .annotationJobWithoutObjectsId(withoutObjects)
            .build());

    verify(jobAnnotationProcessorMock, times(1))
        .accept(
            jobId,
            minConfidence,
            truePositive,
            falsePositive,
            withoutObjects,
            objectConfigurations);
  }

  @Test
  void accept_with_full_detection_object_conf_ok() {
    List<DetectableObjectConfiguration> objectConfigurations =
        List.of(new DetectableObjectConfiguration());
    when(objectConfigurationRepositoryMock.findAllByDetectionJobId(any())).thenReturn(List.of());
    when(detectionRepositoryMock.findByZdjId(any()))
        .thenReturn(
            Optional.of(
                Detection.builder().detectableObjectConfigurations(objectConfigurations).build()));

    String jobId = "jobId";
    String truePositive = "truePositive";
    String falsePositive = "falsePositive";
    String withoutObjects = "withoutObjects";

    subject.accept(
        JobAnnotationProcessed.builder()
            .jobId(jobId)
            .minConfidence(DEFAULT_MIN_CONFIDENCE)
            .annotationJobWithObjectsIdTruePositive(truePositive)
            .annotationJobWithObjectsIdFalsePositive(falsePositive)
            .annotationJobWithoutObjectsId(withoutObjects)
            .build());

    verify(jobAnnotationProcessorMock, times(1))
        .accept(
            jobId,
            DEFAULT_MIN_CONFIDENCE,
            truePositive,
            falsePositive,
            withoutObjects,
            objectConfigurations);
  }
}
