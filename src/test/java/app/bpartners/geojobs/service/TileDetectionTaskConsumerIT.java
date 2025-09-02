package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.model.ModelName.TOITURE;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.DetectableObjectTypeMapper;
import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.file.bucket.CustomBucketComponent;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.MachineDetectedTileRepository;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.detection.FeatureWithDelimitation;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.detection.*;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import app.bpartners.geojobs.service.tiling.TileValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TileDetectionTaskConsumerIT {
  private static final String DEFAULT_API_URL = "";
  GeometryConverter geometryConverter = new GeometryConverter(null);
  MachineDetectedTileRepository machineDetectedTileRepositoryMock = mock();
  ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
  TileObjectDetectorConf tileObjectDetectorConfMock = mock();
  DetectionResponseAggregator detectionResponseAggregator = new DetectionResponseAggregator();
  TileValidator tileValidator = new TileValidator();
  DetectionRepository detectionRepositoryMock = mock();
  CustomBucketComponent customBucketComponentMock = mock();
  DetectionMaskCreator maskCreator = new DetectionMaskCreator();
  GeometryPixelProjector geometryPixelProjector = new GeometryPixelProjector();
  TileCoordinatesPolygonIntersection tilePolygonIntersection =
      new TileCoordinatesPolygonIntersection(geometryPixelProjector, geometryConverter);
  DetectionMaskFromTileRetriever maskRetriever =
      new DetectionMaskFromTileRetriever(maskCreator, tilePolygonIntersection);
  DetectionMapper detectionMapper = new DetectionMapper(tileValidator);
  HttpApiTileObjectDetector objectsDetector =
      new HttpApiTileObjectDetector(
          objectMapper,
          customBucketComponentMock,
          DEFAULT_API_URL,
          tileObjectDetectorConfMock,
          detectionResponseAggregator);

  TileDetectionTaskConsumer subject =
      new TileDetectionTaskConsumer(
          machineDetectedTileRepositoryMock,
          objectsDetector,
          detectionMapper,
          detectionRepositoryMock,
          geometryConverter,
          maskRetriever);

  @Test
  void consume_detection_task_to_real_api() {
    var tileDetectionTaskId = randomUUID().toString();
    var tileId = randomUUID().toString();
    var detectionIdentifier = randomUUID().toString();
    var detectionJobId = randomUUID().toString();
    var parcelJobId = randomUUID().toString();

    var detectionMock = mock(Detection.class);
    var providedFeature = new Feature();
    when(detectionMock.getId()).thenReturn(detectionIdentifier);
    when(detectionMock.hasToitureModelName()).thenReturn(true);
    when(detectionMock.getProvidedGeoJsonZone()).thenReturn(List.of(providedFeature));
    when(detectionMock.getDetectableObjectConfigurations())
        .thenReturn(createDetectableObjectConfigurations(detectionIdentifier, detectionJobId));
    when(detectionMock.getFeatureWithDelimitations())
        .thenReturn(
            List.of(
                new FeatureWithDelimitation(
                    new app.bpartners.geojobs.repository.model.Feature(),
                    List.of(new app.bpartners.geojobs.repository.model.Feature()))) // TODO
            );
    when(detectionRepositoryMock.findByZdjId(detectionJobId))
        .thenReturn(Optional.of(detectionMock));

    assertDoesNotThrow(
        () ->
            subject.accept(
                TileDetectionTask.builder()
                    .id(tileDetectionTaskId)
                    .zoneDetectionJobId(detectionJobId)
                    .detectableObjectConfigurations(
                        createDetectableObjectConfigurations(detectionIdentifier, detectionJobId))
                    .tile(
                        Tile.builder()
                            .id(tileId)
                            .coordinates(new TileCoordinates().x(544680).y(383095).z(20))
                            .build())
                    .jobId(parcelJobId)
                    .build()));
  }

  private List<DetectableObjectConfiguration> createDetectableObjectConfigurations(
      String detectionIdentifier, String detectionJobId) {
    var detectableObjectTypes =
        new DetectableObjectTypeMapper()
            .mapFromModel(new DetectableObjectModel().modelName(TOITURE));
    return detectableObjectTypes.stream()
        .map(
            detectableObjectType -> {
              var detectableObjectConfigurationId = randomUUID().toString();
              return new DetectableObjectConfiguration(
                  detectableObjectConfigurationId,
                  detectionJobId,
                  detectionIdentifier,
                  DetectableType.valueOf(detectableObjectType.getValue()),
                  null,
                  0.0);
            })
        .toList();
  }
}
