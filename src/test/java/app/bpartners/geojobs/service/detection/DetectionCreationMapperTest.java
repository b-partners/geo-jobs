package app.bpartners.geojobs.service.detection;

import static app.bpartners.geojobs.endpoint.rest.model.ModelName.TOITURE;
import static app.bpartners.geojobs.endpoint.rest.model.ModelName.VEGETATION;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
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
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.service.BuildingFinder;
import app.bpartners.geojobs.service.detection.inference.ModelCardResolver;
import app.bpartners.geojobs.service.geodata.MapLayerResolver;
import app.bpartners.geojobs.service.geodata.model.GeodataMapLayer;
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
  void refuses_a_layer_whose_precision_is_available_for_none_of_the_objects() {
    givenLayerWithPrecision(10);
    when(modelCardResolverMock.configurationsAvailableAt(any(), eq(10.0))).thenReturn(List.of());
    when(modelCardResolverMock.supportedSpatialResolutionsInCm(any()))
        .thenReturn(new LinkedHashSet<>(List.of(5.0, 20.0)));

    var exception =
        assertThrows(
            ApiException.class,
            () -> subject.apply(pointDetection(), randomUUID().toString(), null, false, null));

    assertTrue(exception.getMessage().contains("image resolution must be 5 or 20 cm"));
    assertTrue(exception.getMessage().contains("precisionLevelInCm = 10"));
  }

  @Test
  void keeps_every_object_when_all_of_them_are_available_at_the_layer_precision() {
    givenLayerWithPrecision(10);
    when(modelCardResolverMock.configurationsAvailableAt(any(), eq(10.0)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var actual = subject.apply(pointDetection(), randomUUID().toString(), null, false, null);

    assertFalse(actual.getDetectableObjectConfigurations().isEmpty());
    verify(geoServerConfigurationMock).defaultGeoServerProperties("layer", 10);
  }

  @Test
  @SuppressWarnings("unchecked")
  void keeps_only_the_objects_available_at_the_layer_precision() {
    givenLayerWithPrecision(20);
    when(modelCardResolverMock.configurationsAvailableAt(any(), eq(20.0)))
        .thenAnswer(
            invocation ->
                ((List<DetectableObjectConfiguration>) invocation.getArgument(0))
                    .stream().limit(1).toList());

    var actual = subject.apply(pointDetection(), randomUUID().toString(), null, false, null);

    assertEquals(1, actual.getDetectableObjectConfigurations().size());
    verify(geoServerConfigurationMock).defaultGeoServerProperties("layer", 20);
  }

  @Test
  void refuses_a_layer_without_precision() {
    givenLayerWithPrecision(null);
    when(modelCardResolverMock.supportedSpatialResolutionsInCm(any())).thenReturn(Set.of(5.0));

    assertThrows(
        ApiException.class,
        () -> subject.apply(pointDetection(), randomUUID().toString(), null, false, null));
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
