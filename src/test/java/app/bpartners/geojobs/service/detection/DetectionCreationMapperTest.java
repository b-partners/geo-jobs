package app.bpartners.geojobs.service.detection;

import static app.bpartners.geojobs.endpoint.rest.model.ModelName.TOITURE;
import static app.bpartners.geojobs.endpoint.rest.model.ModelName.VEGETATION;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.DetectableObjectTypeMapper;
import app.bpartners.geojobs.endpoint.rest.model.CreateDetection;
import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectModel;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.FeatureGeometry;
import app.bpartners.geojobs.endpoint.rest.model.Point;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.service.geodata.model.GeodataMapLayer;
import app.bpartners.geojobs.service.BuildingFinder;
import app.bpartners.geojobs.service.detection.inference.ModelCardResolver;
import app.bpartners.geojobs.service.geodata.MapLayerResolver;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import app.bpartners.geojobs.service.geoserver.GeoServerConfiguration;
import app.bpartners.geojobs.validator.FeatureTypeChecker;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DetectionCreationMapperTest {

  DetectableObjectTypeMapper detectableObjectTypeMapper = new DetectableObjectTypeMapper();
  FeatureTypeChecker featureTypeChecker = new FeatureTypeChecker();
  GeometryConverter geometryConverterMock = mock();
  BuildingFinder buildingFinderMock = mock();
  MapLayerResolver mapLayerResolverMock = mock();
  GeoServerConfiguration geoServerConfigurationMock = mock();
  ModelCardResolver modelCardResolverMock = mock();

  DetectionCreationMapper subject =
      new DetectionCreationMapper(
          detectableObjectTypeMapper,
          featureTypeChecker,
          mapLayerResolverMock,
          modelCardResolverMock,
          geoServerConfigurationMock,
          geometryConverterMock,
          buildingFinderMock);

  @Test
  void map_detection_with_detectable_object_model_list_ok() {
    var detectionE2Id = randomUUID().toString();
    var communityOwnerId = randomUUID().toString();
    var createDetection = createDetection();

    var actual = subject.apply(createDetection, detectionE2Id, communityOwnerId, false, null);

    assertTrue(actual.getDetectableObjectModelList().containsAll(detectableObjectModelList()));
    assertFalse(actual.getDetectableObjectConfigurations().isEmpty()); // TODO
  }

  @Test
  void map_detection_with_no_detectable_object_model_list_ok() {
    var detectionE2Id = randomUUID().toString();
    var communityOwnerId = randomUUID().toString();
    var createDetection = createDetection().detectableObjectModelList(null);

    var actual = subject.apply(createDetection, detectionE2Id, communityOwnerId, false, null);

    assertNotNull(actual.getDetectableObjectModel());
    assertFalse(actual.getDetectableObjectConfigurations().isEmpty());
  }

  @Test
  void refuses_a_layer_whose_precision_differs_from_the_required_resolution() {
    givenLayerWithPrecision(10);
    when(modelCardResolverMock.requiredSpatialResolutionsInCm(any())).thenReturn(Set.of(5.0));

    var exception =
        assertThrows(
            ApiException.class,
            () -> subject.apply(pointDetection(), randomUUID().toString(), null, false, null));

    assertTrue(exception.getMessage().contains("image resolution must be 5 cm"));
    assertTrue(exception.getMessage().contains("precisionLevelInCm = 10"));
  }

  @Test
  void accepts_a_layer_matching_the_resolution_required_by_the_mapping() {
    givenLayerWithPrecision(10);
    when(modelCardResolverMock.requiredSpatialResolutionsInCm(any())).thenReturn(Set.of(10.0));

    assertDoesNotThrow(
        () -> subject.apply(pointDetection(), randomUUID().toString(), null, false, null));

    verify(geoServerConfigurationMock).defaultGeoServerProperties("layer", 10);
  }

  @Test
  void refuses_when_objects_require_different_resolutions() {
    givenLayerWithPrecision(5);
    when(modelCardResolverMock.requiredSpatialResolutionsInCm(any()))
        .thenReturn(new LinkedHashSet<>(List.of(5.0, 10.0)));

    var exception =
        assertThrows(
            ApiException.class,
            () -> subject.apply(pointDetection(), randomUUID().toString(), null, false, null));

    assertTrue(exception.getMessage().contains("image resolution must be 5 or 10 cm"));
  }

  @Test
  void refuses_a_position_without_any_imagery_layer() {
    when(mapLayerResolverMock.apply(anyDouble(), anyDouble())).thenReturn(Optional.empty());

    assertThrows(
        BadRequestException.class,
        () -> subject.apply(pointDetection(), randomUUID().toString(), null, false, null));
  }

  private void givenLayerWithPrecision(Integer precisionInCm) {
    when(mapLayerResolverMock.apply(anyDouble(), anyDouble()))
        .thenReturn(
            Optional.of(
                new GeodataMapLayer("id", "layer", 2024, "GEOSERVER", null, null, precisionInCm)));
  }

  private CreateDetection pointDetection() {
    var point = new Point().coordinates(List.of(BigDecimal.valueOf(2.3), BigDecimal.valueOf(48.8)));
    point.setType(Point.TypeEnum.POINT);
    return new CreateDetection()
        .detectableObjectModel(new DetectableObjectModel().modelName(VEGETATION))
        .emailReceiver("<EMAIL>")
        .geoJsonZone(List.of(new Feature().geometry(new FeatureGeometry(point))));
  }

  private CreateDetection createDetection() {
    return new CreateDetection()
        .detectableObjectModel(detectableObjectModel())
        .detectableObjectModelList(detectableObjectModelList())
        .emailReceiver("<EMAIL>");
  }

  private List<DetectableObjectModel> detectableObjectModelList() {
    return List.of(
        new DetectableObjectModel().modelName(TOITURE),
        new DetectableObjectModel().modelName(VEGETATION));
  }

  private DetectableObjectModel detectableObjectModel() {
    return new DetectableObjectModel().modelName(TOITURE);
  }
}
