package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.TOITURE_REVETEMENT;
import static app.bpartners.geojobs.service.event.ZoneDetectionFinishedConsumer.DEFAULT_MIN_CONFIDENCE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.repository.DetectableObjectConfigurationRepository;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.Detection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import lombok.NonNull;
import org.junit.jupiter.api.Test;

class ZoneDetectionFinishedConsumerTest {
  private static final String DETECTION_JOB_ID = "detectionJobId";
  ZoneDetectionJobAnnotationProcessor jobAnnotationProcessorMock = mock();
  DetectableObjectConfigurationRepository objectConfigurationRepositoryMock = mock();
  DetectionRepository detectionRepositoryMock = mock();
  ZoneDetectionFinishedConsumer subject =
      new ZoneDetectionFinishedConsumer(
          jobAnnotationProcessorMock, detectionRepositoryMock, objectConfigurationRepositoryMock);

  @Test
  void process_without_detection_ok() {
    List<DetectableObjectConfiguration> objectConfigurations = getObjectConfigurations();
    when(objectConfigurationRepositoryMock.findAllByDetectionJobId(DETECTION_JOB_ID))
        .thenReturn(objectConfigurations);
    AtomicReference<String> annotationJobWithObjectsIdTruePositive = new AtomicReference<>();
    AtomicReference<String> annotationJobWithObjectsIdFalsePositive = new AtomicReference<>();
    AtomicReference<String> annotationJobWithoutObjectsId = new AtomicReference<>();
    when(jobAnnotationProcessorMock.accept(any(), any(), any(), any(), any(), any()))
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
            annotationJobWithoutObjectsId.get(),
            objectConfigurations);
  }

  @Test
  void process_with_detection_ok() {
    var detectableObjectConfigurations = detectableObjectConfigurations();
    when(objectConfigurationRepositoryMock.findAllByDetectionJobId(DETECTION_JOB_ID))
        .thenReturn(getObjectConfigurations());
    when(detectionRepositoryMock.findByZdjId(DETECTION_JOB_ID))
        .thenReturn(
            Optional.of(
                Detection.builder()
                    .detectableObjectConfigurations(detectableObjectConfigurations)
                    .build()));
    AtomicReference<String> annotationJobWithObjectsIdTruePositive = new AtomicReference<>();
    AtomicReference<String> annotationJobWithObjectsIdFalsePositive = new AtomicReference<>();
    AtomicReference<String> annotationJobWithoutObjectsId = new AtomicReference<>();
    when(jobAnnotationProcessorMock.accept(any(), any(), any(), any(), any(), any()))
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
            annotationJobWithoutObjectsId.get(),
            detectableObjectConfigurations);
  }

  @NonNull
  private static List<DetectableObjectConfiguration> detectableObjectConfigurations() {
    return List.of(
        DetectableObjectConfiguration.builder()
            .objectType(TOITURE_REVETEMENT)
            .minConfidenceForDetection(DEFAULT_MIN_CONFIDENCE)
            .build());
  }

  private DetectableObjectConfiguration customDetectableObjectConfiguration() {
    return DetectableObjectConfiguration.builder()
        .objectType(TOITURE_REVETEMENT)
        .minConfidenceForDetection(DEFAULT_MIN_CONFIDENCE)
        .build();
  }

  @NonNull
  private List<DetectableObjectConfiguration> getObjectConfigurations() {
    return List.of(new DetectableObjectConfiguration());
  }
}
