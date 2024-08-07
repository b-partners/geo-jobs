package app.bpartners.geojobs.service.event;

import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.model.annotation.JobAnnotationProcessed;
import org.junit.jupiter.api.Test;

public class JobAnnotationProcessedServiceTest {
  ZoneDetectionJobAnnotationProcessor jobAnnotationProcessorMock = mock();
  JobAnnotationProcessedService subject =
      new JobAnnotationProcessedService(jobAnnotationProcessorMock);

  @Test
  void accept_ok() {
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
        .accept(jobId, minConfidence, truePositive, falsePositive, withoutObjects);
  }
}
