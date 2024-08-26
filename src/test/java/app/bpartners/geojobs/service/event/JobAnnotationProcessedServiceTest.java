package app.bpartners.geojobs.service.event;

import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.model.annotation.JobAnnotationProcessed;
import app.bpartners.geojobs.repository.DetectableObjectConfigurationRepository;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import java.util.List;
import org.junit.jupiter.api.Test;

class JobAnnotationProcessedServiceTest {
  ZoneDetectionJobAnnotationProcessor jobAnnotationProcessorMock = mock();
  DetectableObjectConfigurationRepository objectConfigurationRepositoryMock = mock();
  JobAnnotationProcessedService subject =
      new JobAnnotationProcessedService(
          jobAnnotationProcessorMock, objectConfigurationRepositoryMock);

  @Test
  void accept_ok() {
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
}
