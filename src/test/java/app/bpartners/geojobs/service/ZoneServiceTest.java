package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.model.DetectionStep.CONFIGURING;
import static app.bpartners.geojobs.endpoint.rest.model.Status.HealthEnum.UNKNOWN;
import static app.bpartners.geojobs.endpoint.rest.model.Status.ProgressionEnum.PROCESSING;
import static app.bpartners.geojobs.file.hash.FileHashAlgorithm.SHA256;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.DetectionSaved;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.DetectableObjectTypeMapper;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.DetectionStepStatisticMapper;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.StatusMapper;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.ZoneTilingJobMapper;
import app.bpartners.geojobs.endpoint.rest.model.Detection;
import app.bpartners.geojobs.endpoint.rest.model.DetectionStepStatus;
import app.bpartners.geojobs.endpoint.rest.model.Status;
import app.bpartners.geojobs.endpoint.rest.validator.ZoneDetectionJobValidator;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.file.hash.FileHash;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.ZoneDetectionJobRepository;
import app.bpartners.geojobs.repository.ZoneTilingJobRepository;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.service.geojson.GeoJsonConversionInitiationService;
import app.bpartners.geojobs.service.tiling.ZoneTilingJobService;
import app.bpartners.geojobs.utils.FeatureCreator;
import app.bpartners.geojobs.utils.detection.DetectionCreator;
import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ZoneServiceTest {
  ZoneTilingJobService tilingJobServiceMock = mock();
  ZoneTilingJobMapper tilingJobMapperMock = mock();
  ZoneDetectionJobValidator detectionJobValidatorMock = mock();
  EventProducer eventProducerMock = mock();
  DetectionStepStatisticMapper stepStatisticMapper =
      new DetectionStepStatisticMapper(new StatusMapper<>());
  ZoneDetectionJobRepository zoneDetectionJobRepositoryMock = mock();
  DetectionRepository detectionRepositoryMock = mock();
  ZoneTilingJobRepository tilingJobRepositoryMock = mock();
  CommunityUsedSurfaceService communityUsedSurfaceServiceMock = mock();
  BucketComponent bucketComponentMock = mock();
  GeoJsonConversionInitiationService conversionInitiationServiceMock = mock();
  DetectableObjectTypeMapper detectableObjectTypeMapper = new DetectableObjectTypeMapper();
  ZoneDetectionJobService zoneDetectionJobServiceMock = mock();
  FeatureCreator featureCreator = new FeatureCreator();
  DetectionCreator detectionCreator = new DetectionCreator(featureCreator);

  ZoneService subject =
      new ZoneService(
          zoneDetectionJobServiceMock,
          tilingJobServiceMock,
          tilingJobMapperMock,
          detectionJobValidatorMock,
          eventProducerMock,
          stepStatisticMapper,
          zoneDetectionJobRepositoryMock,
          detectionRepositoryMock,
          tilingJobRepositoryMock,
          communityUsedSurfaceServiceMock,
          bucketComponentMock,
          conversionInitiationServiceMock,
          detectableObjectTypeMapper);

  @SneakyThrows
  @Test
  void configure_shape_file_ok() {
    var shapeFile = File.createTempFile(randomUUID().toString(), randomUUID().toString());
    var detection =
        detectionCreator.createFromZTJAndZDJ(randomUUID().toString(), randomUUID().toString());
    var detectionId = detection.getId();
    var shapeFileBucketKey = "detections/shape/" + detectionId;
    var shapeUrl = "https://localhost";
    when(bucketComponentMock.upload(shapeFile, shapeFileBucketKey))
        .thenReturn(new FileHash(SHA256, "dummy"));
    when(bucketComponentMock.presign(any(), any())).thenReturn(new URI(shapeUrl).toURL());
    when(detectionRepositoryMock.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    when(detectionRepositoryMock.findById(detectionId)).thenReturn(Optional.of(detection));

    var actual = subject.configureShapeFile(detectionId, shapeFile);

    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock, only()).accept(listCaptor.capture());
    var detectionSaved = (DetectionSaved) listCaptor.getValue().getFirst();
    var expectedSavedDetection = detection.toBuilder().shapeFileKey(shapeFileBucketKey).build();
    var expectedDetectionSavedEvent =
        DetectionSaved.builder().detection(expectedSavedDetection).build();
    var expectedRestDetection =
        new Detection()
            .id(detectionId)
            .shapeUrl(shapeUrl)
            .geoJsonZone(detection.getGeoJsonZone())
            .overallConfiguration(detection.getDetectionOverallConfiguration())
            .detectableObjectConfiguration(detection.getCreateMachineDetection())
            .step(
                new DetectionStepStatus()
                    .step(CONFIGURING)
                    .status(
                        new Status()
                            .progression(PROCESSING)
                            .health(UNKNOWN)
                            .creationDatetime(actual.getStep().getStatus().getCreationDatetime()))
                    .statistics(List.of())
                    .updatedAt(actual.getStep().getUpdatedAt()));
    assertEquals(expectedDetectionSavedEvent, detectionSaved);
    assertEquals(expectedRestDetection, actual);
  }
}
