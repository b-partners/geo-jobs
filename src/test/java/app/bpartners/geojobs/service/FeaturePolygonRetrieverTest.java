package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.model.DelimitationType.PARCEL;
import static app.bpartners.geojobs.endpoint.rest.model.DelimitationType.PARCEL_CONSTRAINED_DELIMITATION;
import static app.bpartners.geojobs.endpoint.rest.model.DelimitationType.ZONE;
import static app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.MULTI_POLYGON;
import static app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.POINT;
import static app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.POLYGON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.FeatureGeometry;
import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.endpoint.rest.model.Point;
import app.bpartners.geojobs.endpoint.rest.model.Polygon;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import app.bpartners.geojobs.service.ign.IgnCadastreFeatureFetcher;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

class FeaturePolygonRetrieverTest {
  GeometryConverter geometryConverter = new GeometryConverter();
  IgnCadastreFeatureFetcher ignCadastreFeatureFetcherMock = mock();
  BuildingFinder buildingFinderMock = mock();
  FeaturePolygonRetriever subject =
      new FeaturePolygonRetriever(
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

  private static Feature polygonFeature() {
    return new Feature()
        .properties(new HashMap<>(Map.of("name", "polygon")))
        .geometry(
            new FeatureGeometry(
                new Polygon()
                    .type(Polygon.TypeEnum.POLYGON)
                    .coordinates(List.of(squareRing(0, 0, 10)))));
  }

  private static Feature multiPolygonFeature(int polygons) {
    var coordinates =
        polygons == 1
            ? List.of(List.of(squareRing(0, 0, 10)))
            : List.of(List.of(squareRing(0, 0, 10)), List.of(squareRing(50, 50, 10)));
    return new Feature()
        .properties(new HashMap<>(Map.of("name", "multipolygon")))
        .geometry(
            new FeatureGeometry(
                new MultiPolygon()
                    .type(MultiPolygon.TypeEnum.MULTI_POLYGON)
                    .coordinates(coordinates)));
  }

  private static Feature pointFeature() {
    return new Feature()
        .properties(new HashMap<>(Map.of("name", "point")))
        .geometry(
            new FeatureGeometry(
                new Point().type(Point.TypeEnum.POINT).coordinates(xy(2.35, 48.85))));
  }

  private static app.bpartners.geojobs.repository.model.Feature parcel(
      app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum type, String geometryJson) {
    return app.bpartners.geojobs.repository.model.Feature.builder()
        .geometry(
            app.bpartners.geojobs.repository.model.Feature.FeatureGeometry.builder()
                .geometryType(type)
                .actualInstanceStringValue(geometryJson)
                .build())
        .build();
  }

  private static final String POLYGON_PARCEL =
      "{\"type\":\"Polygon\",\"coordinates\":[[[0,0],[20,0],[20,20],[0,20],[0,0]]]}";
  private static final String MULTI_POLYGON_PARCEL =
      "{\"type\":\"MultiPolygon\",\"coordinates\":[[[[0,0],[30,0],[30,30],[0,30],[0,0]]]]}";

  private static org.locationtech.jts.geom.MultiPolygon building() {
    var factory = new GeometryFactory();
    var polygon =
        factory.createPolygon(
            new Coordinate[] {
              new Coordinate(0, 0),
              new Coordinate(40, 0),
              new Coordinate(40, 40),
              new Coordinate(0, 40),
              new Coordinate(0, 0)
            });
    return factory.createMultiPolygon(new org.locationtech.jts.geom.Polygon[] {polygon});
  }

  private static int firstRingSize(Feature feature) {
    return feature.getGeometry().getPolygon().getCoordinates().getFirst().size();
  }

  @Test
  void polygon_feature_is_returned_as_is() {
    var feature = polygonFeature();

    var actual = subject.apply(feature, ZONE);

    assertSame(feature, actual);
  }

  @Test
  void multi_polygon_feature_becomes_its_first_polygon_with_same_properties() {
    var feature = multiPolygonFeature(2);

    var actual = subject.apply(feature, ZONE);

    assertEquals(Feature.TypeEnum.FEATURE, actual.getType());
    assertEquals(POLYGON.getValue(), actual.getGeometry().getPolygon().getType().getValue());
    assertEquals(Map.of("name", "multipolygon"), actual.getProperties());
    assertEquals(
        squareRing(0, 0, 10), actual.getGeometry().getPolygon().getCoordinates().getFirst());
  }

  @Test
  void single_polygon_multi_polygon_feature_becomes_a_polygon_feature() {
    var actual = subject.apply(multiPolygonFeature(1), ZONE);

    assertEquals(1, actual.getGeometry().getPolygon().getCoordinates().size());
    assertEquals(5, firstRingSize(actual));
  }

  @Test
  void point_feature_gets_the_building_around_it_as_polygon_when_not_parcel_delimitation() {
    when(buildingFinderMock.getBuildingMultiPolygon(any(Point.class))).thenReturn(building());

    var actual = subject.apply(pointFeature(), ZONE);

    assertEquals(Map.of("name", "point"), actual.getProperties());
    assertEquals(5, firstRingSize(actual));
    verifyNoInteractions(ignCadastreFeatureFetcherMock);
  }

  @Test
  void point_feature_gets_the_polygon_parcel_around_it_for_parcel_delimitation() {
    when(ignCadastreFeatureFetcherMock.apply(any()))
        .thenReturn(List.of(parcel(POLYGON, POLYGON_PARCEL)));

    var actual = subject.apply(pointFeature(), PARCEL);

    assertEquals(
        squareRing(0, 0, 20),
        actual.getGeometry().getPolygon().getCoordinates().getFirst().stream()
            .map(p -> xy(p.get(0).doubleValue(), p.get(1).doubleValue()))
            .toList());
    verifyNoInteractions(buildingFinderMock);
  }

  @Test
  void point_feature_gets_the_first_polygon_of_a_multi_polygon_parcel() {
    when(ignCadastreFeatureFetcherMock.apply(any()))
        .thenReturn(
            List.of(parcel(MULTI_POLYGON, MULTI_POLYGON_PARCEL), parcel(POLYGON, POLYGON_PARCEL)));

    var actual = subject.apply(pointFeature(), PARCEL_CONSTRAINED_DELIMITATION);

    assertEquals(5, firstRingSize(actual));
    assertEquals(
        30.0,
        actual
            .getGeometry()
            .getPolygon()
            .getCoordinates()
            .getFirst()
            .get(1)
            .getFirst()
            .doubleValue());
  }

  @Test
  void point_feature_with_unsupported_parcel_geometry_is_rejected() {
    when(ignCadastreFeatureFetcherMock.apply(any()))
        .thenReturn(List.of(parcel(POINT, "{\"type\":\"Point\",\"coordinates\":[1,1]}")));
    var feature = pointFeature();

    var error = assertThrows(IllegalStateException.class, () -> subject.apply(feature, PARCEL));

    assertTrue(error.getMessage().startsWith("Unexpected geometry type"));
  }
}
