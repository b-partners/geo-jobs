package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.MULTI_POLYGON;
import static app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.POINT;
import static app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.POLYGON;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.PISCINE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.TOITURE_REVETEMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.FeatureGeometry;
import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.endpoint.rest.model.Point;
import app.bpartners.geojobs.endpoint.rest.model.Polygon;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

class GeometryConverterConversionsTest {
  private static final GeometryFactory JTS = new GeometryFactory();
  private static final double TOLERANCE = 0.0001;

  GeometryConverter subject = new GeometryConverter();

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

  private static List<List<BigDecimal>> openSquareRing(double minX, double minY, double size) {
    var ring = new ArrayList<>(squareRing(minX, minY, size));
    ring.removeLast();
    return ring;
  }

  private static org.locationtech.jts.geom.Polygon jtsSquare(
      double minX, double minY, double size) {
    return JTS.createPolygon(
        new Coordinate[] {
          new Coordinate(minX, minY),
          new Coordinate(minX + size, minY),
          new Coordinate(minX + size, minY + size),
          new Coordinate(minX, minY + size),
          new Coordinate(minX, minY)
        });
  }

  private static org.locationtech.jts.geom.MultiPolygon jtsMultiPolygon(
      org.locationtech.jts.geom.Polygon... polygons) {
    return JTS.createMultiPolygon(polygons);
  }

  private static Feature restFeature(Polygon polygon) {
    return new Feature().geometry(new FeatureGeometry(polygon));
  }

  private static Feature restFeature(MultiPolygon multiPolygon) {
    return new Feature().geometry(new FeatureGeometry(multiPolygon));
  }

  private static Feature restFeature(Point point) {
    return new Feature().geometry(new FeatureGeometry(point));
  }

  private static Polygon restPolygon(double minX, double minY, double size) {
    return new Polygon()
        .type(Polygon.TypeEnum.POLYGON)
        .coordinates(List.of(squareRing(minX, minY, size)));
  }

  private static MultiPolygon restMultiPolygon(double minX, double minY, double size) {
    return new MultiPolygon()
        .type(MultiPolygon.TypeEnum.MULTI_POLYGON)
        .coordinates(List.of(List.of(squareRing(minX, minY, size))));
  }

  private static Point restPoint(double x, double y) {
    return new Point().type(Point.TypeEnum.POINT).coordinates(xy(x, y));
  }

  private static app.bpartners.geojobs.repository.model.Feature domainFeature(
      app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum type, String geometryJson) {
    return app.bpartners.geojobs.repository.model.Feature.builder()
        .geometry(
            app.bpartners.geojobs.repository.model.Feature.FeatureGeometry.builder()
                .geometryType(type)
                .actualInstanceStringValue(geometryJson)
                .build())
        .build();
  }

  @Test
  void to_feature_from_multi_polygon_keeps_id_zoom_properties_and_geometry() {
    var properties = new HashMap<String, Object>(Map.of("label", "bati"));
    var multiPolygon = jtsMultiPolygon(jtsSquare(0, 0, 10));

    var actual = subject.toFeature("featureId", 20, properties, multiPolygon);

    assertEquals("featureId", actual.getId());
    assertEquals(20, actual.getZoom());
    assertEquals(MULTI_POLYGON, actual.getGeometry().getGeometryType());
    assertTrue(actual.getGeometry().getActualInstanceStringValue().contains("MultiPolygon"));
    assertEquals(Map.of("label", "bati"), actual.getProperties());
  }

  @Test
  void to_feature_from_point_keeps_id_zoom_properties_and_geometry() {
    var properties = new HashMap<String, Object>(Map.of("address", "1 rue de Paris"));
    var point = JTS.createPoint(new Coordinate(2.35, 48.85));

    var actual = subject.toFeature("pointId", 18, properties, point);

    assertEquals("pointId", actual.getId());
    assertEquals(18, actual.getZoom());
    assertEquals(POINT, actual.getGeometry().getGeometryType());
    assertTrue(actual.getGeometry().getActualInstanceStringValue().contains("Point"));
    assertEquals(Map.of("address", "1 rue de Paris"), actual.getProperties());
  }

  @Test
  void rest_multi_polygon_from_jts_keeps_coordinates() {
    var actual = subject.restMultiPolygonFromJts(jtsMultiPolygon(jtsSquare(1, 2, 10)));

    assertEquals(1, actual.getCoordinates().size());
    var shell = actual.getCoordinates().getFirst().getFirst();
    assertEquals(5, shell.size());
    assertEquals(0, BigDecimal.valueOf(1).compareTo(shell.getFirst().getFirst()));
    assertEquals(0, BigDecimal.valueOf(2).compareTo(shell.getFirst().getLast()));
  }

  @Test
  void centroid_of_rest_geometries_is_computed_from_their_coordinates() {
    var fromPolygon = subject.centroidFromGeometry(restPolygon(0, 0, 10));
    var fromMultiPolygon = subject.centroidFromGeometry(restMultiPolygon(0, 0, 10));
    var fromPoint = subject.centroidFromGeometry(restPoint(3, 4));

    assertEquals(5.0, fromPolygon.getFirst().doubleValue(), TOLERANCE);
    assertEquals(5.0, fromPolygon.getLast().doubleValue(), TOLERANCE);
    assertEquals(5.0, fromMultiPolygon.getFirst().doubleValue(), TOLERANCE);
    assertEquals(3.0, fromPoint.getFirst().doubleValue(), TOLERANCE);
    assertEquals(4.0, fromPoint.getLast().doubleValue(), TOLERANCE);
  }

  @Test
  void centroid_of_jts_geometries_is_computed_directly() {
    var fromPolygon = subject.centroidFromGeometry(jtsSquare(0, 0, 10));
    var fromMultiPolygon = subject.centroidFromGeometry(jtsMultiPolygon(jtsSquare(0, 0, 20)));
    var fromPoint = subject.centroidFromGeometry(JTS.createPoint(new Coordinate(7, 8)));

    assertEquals(5.0, fromPolygon.getFirst().doubleValue(), TOLERANCE);
    assertEquals(10.0, fromMultiPolygon.getFirst().doubleValue(), TOLERANCE);
    assertEquals(7.0, fromPoint.getFirst().doubleValue(), TOLERANCE);
    assertEquals(8.0, fromPoint.getLast().doubleValue(), TOLERANCE);
  }

  @Test
  void centroid_of_unsupported_geometry_is_rejected() {
    assertThrows(UnsupportedOperationException.class, () -> subject.centroidFromGeometry("nope"));
  }

  @Test
  void polygon_to_points_lists_the_exterior_ring_coordinates() {
    var actual = subject.polygonToPoints(jtsSquare(0, 0, 10));

    assertEquals(5, actual.size());
    assertEquals(0, BigDecimal.ZERO.compareTo(actual.getFirst().getFirst()));
    assertEquals(actual.getFirst(), actual.getLast());
  }

  @Test
  void static_write_geometry_as_string_serializes_geojson() {
    var actual =
        GeometryConverter.staticWriteGeometryAsString(JTS.createPoint(new Coordinate(2, 3)));

    assertEquals("{\"type\":\"Point\",\"coordinates\":[2,3]}", actual);
  }

  @Test
  void read_geometry_from_string_with_explicit_decimals_reads_a_point() {
    var actual =
        subject.readGeometryFromString("{\"type\":\"Point\",\"coordinates\":[2.123456,3]}", 3);

    assertEquals(2.123456, ((org.locationtech.jts.geom.Point) actual).getX(), TOLERANCE);
  }

  @Test
  void convert_open_ring_closes_it_before_building_the_polygon() {
    var actual = subject.convertToPolygon(openSquareRing(0, 0, 10));

    assertTrue(actual.isValid());
    assertEquals(100.0, actual.getArea(), TOLERANCE);
  }

  @Test
  void convert_rings_builds_polygons_with_holes_and_closes_open_rings() {
    var shell = openSquareRing(0, 0, 10);
    var hole = openSquareRing(2, 2, 2);

    var actual = subject.convertRingsToPolygons(List.of(shell, hole));

    assertEquals(1, actual.size());
    assertEquals(1, actual.getFirst().getNumInteriorRing());
    assertEquals(96.0, actual.getFirst().getArea(), TOLERANCE);
  }

  @Test
  void convert_rings_rejects_missing_rings() {
    List<List<List<BigDecimal>>> noRings = List.of();

    assertThrows(IllegalArgumentException.class, () -> subject.convertRingsToPolygons(noRings));
    assertThrows(IllegalArgumentException.class, () -> subject.convertRingsToPolygons(null));
  }

  @Test
  void convert_rings_rejects_points_with_less_than_two_coordinates() {
    var invalidRing = List.of(List.of(BigDecimal.ONE), xy(1, 1), xy(2, 2), xy(3, 3));
    var rings = List.of(invalidRing);

    assertThrows(IllegalArgumentException.class, () -> subject.convertRingsToPolygons(rings));
  }

  @Test
  void widest_polygon_keeps_the_biggest_part_of_a_multi_polygon() {
    var multiPolygon = jtsMultiPolygon(jtsSquare(0, 0, 10), jtsSquare(100, 100, 30));

    var actual = subject.widestPolygonWithSurfaceOf(multiPolygon);

    assertEquals(900.0, actual.getArea(), TOLERANCE);
  }

  @Test
  void widest_polygon_falls_back_to_first_part_when_none_has_surface() {
    var flat =
        JTS.createPolygon(
            new Coordinate[] {
              new Coordinate(0, 0),
              new Coordinate(5, 0),
              new Coordinate(10, 0),
              new Coordinate(0, 0)
            });
    var multiPolygon = jtsMultiPolygon(flat);

    var actual = subject.widestPolygonWithSurfaceOf(multiPolygon);

    assertSame(multiPolygon.getGeometryN(0), actual);
  }

  @Test
  void multi_polygon_to_nested_list_keeps_holes() {
    var shell = JTS.createLinearRing(jtsSquare(0, 0, 10).getCoordinates());
    var hole = JTS.createLinearRing(jtsSquare(2, 2, 2).getCoordinates());
    var withHole = JTS.createPolygon(shell, new org.locationtech.jts.geom.LinearRing[] {hole});

    var actual = subject.multiPolygonToNestedList(jtsMultiPolygon(withHole));

    assertEquals(1, actual.size());
    assertEquals(2, actual.getFirst().size());
    assertEquals(5, actual.getFirst().getLast().size());
  }

  @Test
  void geometry_to_multi_polygon_coordinates_wraps_a_polygon_with_its_holes() {
    var shell = JTS.createLinearRing(jtsSquare(0, 0, 10).getCoordinates());
    var hole = JTS.createLinearRing(jtsSquare(2, 2, 2).getCoordinates());
    var withHole = JTS.createPolygon(shell, new org.locationtech.jts.geom.LinearRing[] {hole});

    var actual = subject.geometryToMultiPolygonCoordinates(withHole);

    assertEquals(1, actual.size());
    assertEquals(2, actual.getFirst().size());
  }

  @Test
  void geometry_to_multi_polygon_coordinates_rejects_points() {
    var point = JTS.createPoint(new Coordinate(1, 1));

    assertThrows(
        UnsupportedOperationException.class,
        () -> subject.geometryToMultiPolygonCoordinates(point));
  }

  @Test
  void unify_multi_polygon_merges_overlapping_polygons_into_one() {
    var overlapping =
        List.of(jtsMultiPolygon(jtsSquare(0, 0, 10)), jtsMultiPolygon(jtsSquare(5, 5, 10)));

    var actual = overlapping.stream().reduce(GeometryConverter.unifyMultiPolygon()).orElseThrow();

    assertEquals(1, actual.getNumGeometries());
    assertEquals(175.0, actual.getArea(), TOLERANCE);
  }

  @Test
  void unify_multi_polygon_keeps_disjoint_polygons_apart() {
    var disjoint =
        List.of(jtsMultiPolygon(jtsSquare(0, 0, 10)), jtsMultiPolygon(jtsSquare(50, 50, 10)));

    var actual = disjoint.stream().reduce(GeometryConverter.unifyMultiPolygon()).orElseThrow();

    assertEquals(2, actual.getNumGeometries());
  }

  @Test
  void retrieve_multi_polygon_from_polygon_and_multi_polygon_features() {
    var fromPolygon =
        GeometryConverter.retrieveMultiPolygonFromFeature(restFeature(restPolygon(0, 0, 10)), null);
    var fromMultiPolygon =
        GeometryConverter.retrieveMultiPolygonFromFeature(
            restFeature(restMultiPolygon(0, 0, 20)), null);

    assertEquals(100.0, fromPolygon.getArea(), TOLERANCE);
    assertEquals(400.0, fromMultiPolygon.getArea(), TOLERANCE);
  }

  @Test
  void retrieve_multi_polygon_from_feature_without_geometry_is_null() {
    assertNull(GeometryConverter.retrieveMultiPolygonFromFeature(new Feature(), null));
  }

  @Test
  void retrieve_multi_polygon_from_point_feature_unifies_its_delimitations() {
    var delimitations =
        List.of(restFeature(restPolygon(0, 0, 10)), restFeature(restMultiPolygon(50, 50, 10)));

    var withDelimitations =
        GeometryConverter.retrieveMultiPolygonFromFeature(
            restFeature(restPoint(1, 1)), delimitations);
    var withoutDelimitations =
        GeometryConverter.retrieveMultiPolygonFromFeature(restFeature(restPoint(1, 1)), null);

    assertEquals(2, withDelimitations.getNumGeometries());
    assertNull(withoutDelimitations);
  }

  @Test
  void roof_multi_polygon_zone_is_built_from_polygon_or_multi_polygon() {
    var fromPolygon =
        GeometryConverter.getRoofMultiPolygonZoneProcessed(restFeature(restPolygon(0, 0, 10)));
    var fromMultiPolygon =
        GeometryConverter.getRoofMultiPolygonZoneProcessed(restFeature(restMultiPolygon(0, 0, 20)));

    assertEquals(100.0, fromPolygon.getArea(), TOLERANCE);
    assertEquals(400.0, fromMultiPolygon.getArea(), TOLERANCE);
  }

  @Test
  void roof_multi_polygon_zone_rejects_other_geometries() {
    var pointFeature = restFeature(restPoint(1, 1));

    assertThrows(
        IllegalStateException.class,
        () -> GeometryConverter.getRoofMultiPolygonZoneProcessed(pointFeature));
  }

  @Test
  void zone_processed_is_the_roof_unless_parcel_detection_targets_a_non_roof_object() {
    var roofFeature = restFeature(restPolygon(0, 0, 10));

    var roofObjectOnParcelDetection =
        GeometryConverter.getMultiPolygonZoneProcessed(roofFeature, true, TOITURE_REVETEMENT);
    var objectWithoutParcelDetection =
        GeometryConverter.getMultiPolygonZoneProcessed(roofFeature, false, PISCINE);

    assertEquals(100.0, roofObjectOnParcelDetection.getArea(), TOLERANCE);
    assertEquals(100.0, objectWithoutParcelDetection.getArea(), TOLERANCE);
  }

  @Test
  void nearest_parcel_multi_polygon_is_built_from_polygon_or_multi_polygon_parcels() {
    var polygonParcel =
        domainFeature(
            POLYGON,
            "{\"type\":\"Polygon\",\"coordinates\":[[[0,0],[10,0],[10,10],[0,10],[0,0]]]}");
    var multiPolygonParcel =
        domainFeature(
            MULTI_POLYGON,
            "{\"type\":\"MultiPolygon\",\"coordinates\":[[[[0,0],[20,0],[20,20],[0,20],[0,0]]]]}");

    var fromPolygon = subject.retrieveNearestParcelMultiPolygon(List.of(polygonParcel));
    var fromMultiPolygon = subject.retrieveNearestParcelMultiPolygon(List.of(multiPolygonParcel));

    assertNotNull(fromPolygon);
    assertEquals(100.0, fromPolygon.getArea(), TOLERANCE);
    assertEquals(400.0, fromMultiPolygon.getArea(), TOLERANCE);
  }

  @Test
  void nearest_parcel_multi_polygon_rejects_other_geometries() {
    var pointParcel = domainFeature(POINT, "{\"type\":\"Point\",\"coordinates\":[1,1]}");
    var parcels = List.of(pointParcel);

    assertThrows(
        IllegalStateException.class, () -> subject.retrieveNearestParcelMultiPolygon(parcels));
  }
}
