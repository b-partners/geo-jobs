package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.service.event.ZoneDetectionFinishedConsumer.DEFAULT_MIN_CONFIDENCE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ZoneDetectionFinishedConsumerTest {
  private static final String DETECTION_JOB_ID = "detectionJobId";
  ZoneDetectionJobAnnotationProcessor jobAnnotationProcessorMock = mock();
  ZoneDetectionFinishedConsumer subject =
      new ZoneDetectionFinishedConsumer(jobAnnotationProcessorMock);

  @Test
  void process_ok() {
    AtomicReference<String> annotationJobWithObjectsIdTruePositive = new AtomicReference<>();
    AtomicReference<String> annotationJobWithObjectsIdFalsePositive = new AtomicReference<>();
    AtomicReference<String> annotationJobWithoutObjectsId = new AtomicReference<>();
    when(jobAnnotationProcessorMock.accept(any(), any(), any(), any(), any()))
        .thenAnswer(
            invocationOnMock -> {
              annotationJobWithObjectsIdTruePositive.set(invocationOnMock.getArgument(2));
              annotationJobWithObjectsIdFalsePositive.set(invocationOnMock.getArgument(3));
              annotationJobWithoutObjectsId.set(invocationOnMock.getArgument(4));
              return new ZoneDetectionJobAnnotationProcessor.AnnotationJobIds(
                  annotationJobWithObjectsIdTruePositive.get(),
                  annotationJobWithObjectsIdFalsePositive.get(),
                  annotationJobWithoutObjectsId.get());
            });

    subject.accept(DETECTION_JOB_ID);

    verify(jobAnnotationProcessorMock, times(1))
        .accept(
            DETECTION_JOB_ID,
            DEFAULT_MIN_CONFIDENCE,
            annotationJobWithObjectsIdTruePositive.get(),
            annotationJobWithObjectsIdFalsePositive.get(),
            annotationJobWithoutObjectsId.get());
  }
}
