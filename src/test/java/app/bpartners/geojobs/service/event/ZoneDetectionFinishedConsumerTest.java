package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.ROOF;
import static app.bpartners.geojobs.service.event.ZoneDetectionFinishedConsumer.DEFAULT_MIN_CONFIDENCE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.DetectableObjectConfigurationMapper;
import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType;
import app.bpartners.geojobs.repository.DetectableObjectConfigurationRepository;
import app.bpartners.geojobs.repository.FullDetectionRepository;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.FullDetection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import lombok.NonNull;
import org.junit.jupiter.api.Test;

class ZoneDetectionFinishedConsumerTest {
  private static final String DETECTION_JOB_ID = "detectionJobId";
  private static final double CUSTOM_CONFIDENCE = 0.92;
  ZoneDetectionJobAnnotationProcessor jobAnnotationProcessorMock = mock();
  DetectableObjectConfigurationRepository objectConfigurationRepositoryMock = mock();
  FullDetectionRepository fullDetectionRepositoryMock = mock();
  DetectableObjectConfigurationMapper objectConfigurationMapperMock = mock();
  ZoneDetectionFinishedConsumer subject =
      new ZoneDetectionFinishedConsumer(
          jobAnnotationProcessorMock,
          fullDetectionRepositoryMock,
          objectConfigurationRepositoryMock,
          objectConfigurationMapperMock);

  @Test
  void process_without_full_detection_ok() {
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
  void process_with_full_detection_ok() {
    var customedDetectableObjectConfiguration = customDetectableObjectConfiguration();
    var restDetectableObjectType =
        new app.bpartners.geojobs.endpoint.rest.model.DetectableObjectConfiguration()
            .type(DetectableObjectType.ROOF);
    when(objectConfigurationRepositoryMock.findAllByDetectionJobId(DETECTION_JOB_ID))
        .thenReturn(getObjectConfigurations());
    when(fullDetectionRepositoryMock.findByZdjId(DETECTION_JOB_ID))
        .thenReturn(
            Optional.of(
                FullDetection.builder()
                    .detectableObjectConfiguration(restDetectableObjectType)
                    .build()));
    when(objectConfigurationMapperMock.toDomain(DETECTION_JOB_ID, restDetectableObjectType))
        .thenReturn(customedDetectableObjectConfiguration);
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
            CUSTOM_CONFIDENCE,
            annotationJobWithObjectsIdTruePositive.get(),
            annotationJobWithObjectsIdFalsePositive.get(),
            annotationJobWithoutObjectsId.get(),
            List.of(customedDetectableObjectConfiguration));
  }

  private DetectableObjectConfiguration customDetectableObjectConfiguration() {
    return DetectableObjectConfiguration.builder()
        .objectType(ROOF)
        .confidence(CUSTOM_CONFIDENCE)
        .build();
  }

  @NonNull
  private List<DetectableObjectConfiguration> getObjectConfigurations() {
    return List.of(new DetectableObjectConfiguration());
  }
}
