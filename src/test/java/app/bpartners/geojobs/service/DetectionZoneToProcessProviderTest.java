package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.model.DelimitationType.PARCEL;
import static app.bpartners.geojobs.endpoint.rest.model.DelimitationType.ZONE;
import static app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.POINT;
import static app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.POLYGON;
import static app.bpartners.geojobs.endpoint.rest.model.ModelName.STATIONNEMENT;
import static app.bpartners.geojobs.endpoint.rest.model.ModelName.TOITURE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectModel;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.FeatureGeometry;
import app.bpartners.geojobs.endpoint.rest.model.ModelName;
import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.endpoint.rest.model.Point;
import app.bpartners.geojobs.endpoint.rest.model.Polygon;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import app.bpartners.geojobs.service.ign.IgnCadastreFeatureFetcher;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

class DetectionZoneToProcessProviderTest {
  private static final double TOLERANCE = 0.0001;

  GeometryConverter geometryConverter = new GeometryConverter();
  IgnCadastreFeatureFetcher ignCadastreFeatureFetcherMock = mock();
  BuildingFinder buildingFinderMock = mock();
  DetectionZoneToProcessProvider subject =
      new DetectionZoneToProcessProvider(
          geometryConverter, ignCadastreFeatureFetcherMock, buildingFinderMock);

  private static List<BigDecimal> xy(double x, double y) {
    return List.of(BigDecimal.valueOf(x), BigDecimal.valueOf(y));
  }

  private static List<List<BigDecimal>> squareRing(double minX, double minY, double size) {
    return List.of(
        xy(minX, minY),
        xy(minX + size, minY),
        xy(minX + size, minY + size),
        xy(minX, minY + size),
        xy(minX, minY));
  }

  private static Feature polygonFeature(double minX, double minY, double size) {
    return new Feature()
        .properties(new HashMap<>(Map.of("name", "polygon")))
        .geometry(
            new FeatureGeometry(
                new Polygon()
                    .type(Polygon.TypeEnum.POLYGON)
                    .coordinates(List.of(squareRing(minX, minY, size)))));
  }

  private static Feature multiPolygonFeature(double minX, double minY, double size) {
    return new Feature()
        .properties(new HashMap<>(Map.of("name", "multipolygon")))
        .geometry(
            new FeatureGeometry(
                new MultiPolygon()
                    .type(MultiPolygon.TypeEnum.MULTI_POLYGON)
                    .coordinates(List.of(List.of(squareRing(minX, minY, size))))));
  }

  private static Feature pointFeature() {
    return new Feature()
        .properties(new HashMap<>(Map.of("name", "point")))
        .geometry(
            new FeatureGeometry(
                new Point().type(Point.TypeEnum.POINT).coordinates(xy(2.35, 48.85))));
  }

  private static app.bpartners.geojobs.repository.model.Feature domainPolygonFeature(
      double minX, double minY, double size) {
    var coordinates =
        squareRing(minX, minY, size).stream()
            .map(p -> "[" + p.get(0) + "," + p.get(1) + "]")
            .toList();
    return app.bpartners.geojobs.repository.model.Feature.builder()
        .properties(new HashMap<>())
        .geometry(
            app.bpartners.geojobs.repository.model.Feature.FeatureGeometry.builder()
                .geometryType(POLYGON)
                .actualInstanceStringValue(
                    "{\"type\":\"Polygon\",\"coordinates\":[["
                        + String.join(",", coordinates)
                        + "]]}")
                .build())
        .build();
  }

  private static app.bpartners.geojobs.repository.model.Feature domainPointFeature() {
    return app.bpartners.geojobs.repository.model.Feature.builder()
        .properties(new HashMap<>())
        .geometry(
            app.bpartners.geojobs.repository.model.Feature.FeatureGeometry.builder()
                .geometryType(POINT)
                .actualInstanceStringValue("{\"type\":\"Point\",\"coordinates\":[2.35,48.85]}")
                .build())
        .build();
  }

  private static org.locationtech.jts.geom.MultiPolygon jtsSquare(double minX, double size) {
    var factory = new GeometryFactory();
    var polygon =
        factory.createPolygon(
            new Coordinate[] {
              new Coordinate(minX, minX),
              new Coordinate(minX + size, minX),
              new Coordinate(minX + size, minX + size),
              new Coordinate(minX, minX + size),
              new Coordinate(minX, minX)
            });
    return factory.createMultiPolygon(new org.locationtech.jts.geom.Polygon[] {polygon});
  }

  private static Detection.DetectionBuilder detection(
      app.bpartners.geojobs.repository.model.Feature... zone) {
    return Detection.builder()
        .id("detectionId")
        .detectableObjectModel(new DetectableObjectModel().modelName(TOITURE))
        .providedGeoJsonZone(List.of(zone));
  }

  @Test
  void null_detection_gives_an_empty_multi_polygon() {
    var actual = subject.apply((Detection) null);

    assertTrue(actual.isEmpty());
  }

  @Test
  void zone_of_a_detection_is_the_union_of_its_polygons_and_multi_polygons() {
    var detection =
        detection(domainPolygonFeature(0, 0, 10), domainPolygonFeature(100, 100, 20)).build();

    var actual = subject.apply(detection);

    assertEquals(2, actual.getNumGeometries());
    assertEquals(500.0, actual.getArea(), TOLERANCE);
  }

  @Test
  void overlapping_zones_are_unified_into_a_single_polygon() {
    var detection =
        detection(domainPolygonFeature(0, 0, 10), domainPolygonFeature(5, 5, 10)).build();

    var actual = subject.apply(detection);

    assertEquals(1, actual.getNumGeometries());
    assertEquals(175.0, actual.getArea(), TOLERANCE);
  }

  @Test
  void zone_is_rejected_when_nothing_can_be_unified() {
    var detection =
        Detection.builder()
            .id("detectionId")
            .detectableObjectModel(new DetectableObjectModel().modelName(STATIONNEMENT))
            .providedGeoJsonZone(List.of(domainPointFeature()))
            .build();

    var error = assertThrows(IllegalArgumentException.class, () -> subject.apply(detection));

    assertEquals(
        "Unable to unify provided zone for detection.id : detectionId", error.getMessage());
  }

  @Test
  void internal_geo_features_of_null_detection_are_empty() {
    assertTrue(subject.applyInternalGeoFeatures(null).isEmpty());
  }

  @Test
  void internal_geo_features_use_every_model_of_the_detection_model_list() {
    var detection =
        Detection.builder()
            .id("detectionId")
            .detectableObjectModelList(
                List.of(
                    new DetectableObjectModel().modelName(STATIONNEMENT),
                    new DetectableObjectModel().modelName(TOITURE)))
            .providedGeoJsonZone(List.of(domainPointFeature()))
            .geoJsonDelimitationType(ZONE)
            .build();
    var building = jtsSquare(0, 10);
    when(buildingFinderMock.getBuildingMultiPolygon(any(Point.class))).thenReturn(building);

    var actual = subject.applyInternalGeoFeatures(detection);

    assertEquals(1, actual.size());
    assertSame(building, actual.getFirst().geometry());
  }

  @Test
  void internal_geo_features_use_the_single_model_when_there_is_no_model_list() {
    var detection = detection(domainPolygonFeature(0, 0, 10)).geoJsonDelimitationType(ZONE).build();

    var actual = subject.applyInternalGeoFeatures(detection);

    assertEquals(1, actual.size());
    assertEquals(100.0, actual.getFirst().geometry().getArea(), TOLERANCE);
  }

  @Test
  void geo_features_of_missing_or_empty_feature_list_are_empty() {
    assertTrue(subject.apply(null, List.of(TOITURE), ZONE).isEmpty());
    assertTrue(subject.apply(List.of(), List.of(TOITURE), ZONE).isEmpty());
  }

  @Test
  void geo_features_skip_features_without_geometry_and_keep_properties_of_the_others() {
    var withoutGeometry = new Feature().properties(new HashMap<>());

    var actual =
        subject.apply(
            List.of(withoutGeometry, polygonFeature(0, 0, 10), multiPolygonFeature(50, 50, 10)),
            List.of(TOITURE),
            ZONE);

    assertEquals(2, actual.size());
    assertEquals(Map.of("name", "polygon"), actual.getFirst().properties());
    assertEquals(100.0, actual.getFirst().geometry().getArea(), TOLERANCE);
    assertEquals(Map.of("name", "multipolygon"), actual.getLast().properties());
  }

  @Test
  void point_is_ignored_for_models_that_do_not_rely_on_roofs() {
    var actual = subject.apply(List.of(pointFeature()), List.of(STATIONNEMENT), ZONE);

    assertTrue(actual.isEmpty());
    verifyNoInteractions(buildingFinderMock);
    verifyNoInteractions(ignCadastreFeatureFetcherMock);
  }

  @Test
  void point_gives_the_building_around_it_for_roof_models_without_parcel_delimitation() {
    var building = jtsSquare(0, 10);
    when(buildingFinderMock.getBuildingMultiPolygon(any(Point.class))).thenReturn(building);

    var actual = subject.apply(List.of(pointFeature()), List.of(TOITURE), ZONE);

    assertEquals(1, actual.size());
    assertSame(building, actual.getFirst().geometry());
    assertEquals(Map.of("name", "point"), actual.getFirst().properties());
    verifyNoInteractions(ignCadastreFeatureFetcherMock);
  }

  @Test
  void point_gives_the_nearest_parcel_for_roof_models_with_parcel_delimitation() {
    var parcels = List.of(domainPolygonFeature(0, 0, 10), domainPolygonFeature(100, 100, 10));
    when(ignCadastreFeatureFetcherMock.apply(any())).thenReturn(parcels);

    var actual = subject.apply(List.of(pointFeature()), List.of(TOITURE), PARCEL);

    assertEquals(1, actual.size());
    assertEquals(100.0, actual.getFirst().geometry().getArea(), TOLERANCE);
    verifyNoInteractions(buildingFinderMock);
  }

  @Test
  void point_without_parcel_around_it_is_ignored_with_parcel_delimitation() {
    when(ignCadastreFeatureFetcherMock.apply(any())).thenReturn(List.of());

    var actual = subject.apply(List.of(pointFeature()), List.of(TOITURE), PARCEL);

    assertTrue(actual.isEmpty());
    verify(ignCadastreFeatureFetcherMock).apply(any());
  }

  @Test
  void polygon_is_kept_whatever_the_model_names() {
    var actual = subject.apply(List.of(polygonFeature(0, 0, 10)), List.<ModelName>of(), ZONE);

    assertEquals(1, actual.size());
  }
}
