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
    var providedFeature = new app.bpartners.geojobs.endpoint.rest.model.Feature();
    when(detectionMock.getId()).thenReturn(detectionIdentifier);
    when(detectionMock.hasToitureModelName()).thenReturn(true);
    when(detectionMock.getProvidedGeoJsonZone()).thenReturn(List.of(providedFeature));
    when(detectionMock.getDetectableObjectConfigurations())
        .thenReturn(createDetectableObjectConfigurations(detectionIdentifier, detectionJobId));

    app.bpartners.geojobs.repository.model.Feature.FeatureGeometry geometry =
        new app.bpartners.geojobs.repository.model.Feature.FeatureGeometry();

    geometry.setGeometryType(
        app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.MULTI_POLYGON);
    geometry.setActualInstanceStringValue(
        """
{
  "type": "Polygon",
  "coordinates": [
    [
      [
        [7.001380920410156, 43.55065076822822],
        [7.001271383246328, 43.550651153395584],
        [7.001269834513988, 43.550646706078226],
        [7.001241150953786, 43.55062614280176],
        [7.001235036387186, 43.550627269241545],
        [7.001234033877726, 43.55061289199318],
        [7.00125133009923, 43.55061225397956],
        [7.00124907444506, 43.55057990517163],
        [7.001235484564402, 43.55058040646799],
        [7.001233497239312, 43.550569669104384],
        [7.001158197839128, 43.55057334756624],
        [7.00116400714788, 43.55063889819283],
        [7.001143004582222, 43.55063967290642],
        [7.001150299949268, 43.55070877227807],
        [7.001380920410156, 43.550711491964144],
        [7.001380920410156, 43.55065076822822]
      ]
    ]
  ]
}
""");

    app.bpartners.geojobs.repository.model.Feature feature =
        new app.bpartners.geojobs.repository.model.Feature();
    feature.setProperties(new java.util.HashMap<>());
    feature.setGeometry(geometry);

    when(detectionMock.getFeatureWithDelimitations())
        .thenReturn(
            List.of(
                new FeatureWithDelimitation(
                    new app.bpartners.geojobs.repository.model.Feature(), List.of(feature)))
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
