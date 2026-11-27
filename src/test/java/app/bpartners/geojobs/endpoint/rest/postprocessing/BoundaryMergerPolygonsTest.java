package app.bpartners.geojobs.endpoint.rest.postprocessing;

import static app.bpartners.geojobs.model.geometry.route.ObjectType.tombe;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.PASSAGE_PIETON;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.TOMBE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.model.geometry.IntXY;
import app.bpartners.geojobs.postprocessing.BoundaryMerger;
import app.bpartners.geojobs.postprocessing.model.LatLonPolygon;
import app.bpartners.geojobs.postprocessing.model.TiledPolygon;
import app.bpartners.geojobs.postprocessing.model.TilingConf;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;

class BoundaryMergerPolygonsTest {
  private static final GeometryFactory FACTORY = new GeometryFactory();
  private static final TilingConf TILING_CONF = new TilingConf(20, 1_024);

  BoundaryMerger subject = new BoundaryMerger(4_000, 41, false);

  private static Polygon square(double minX, double minY, double size, String label) {
    var polygon =
        FACTORY.createPolygon(
            new Coordinate[] {
              new Coordinate(minX, minY),
              new Coordinate(minX + size, minY),
              new Coordinate(minX + size, minY + size),
              new Coordinate(minX, minY + size),
              new Coordinate(minX, minY)
            });
    polygon.setUserData(Map.of("label", label));
    return polygon;
  }

  private static TiledPolygon tombAt(Polygon polygon, int tileX, int tileY) {
    return new TiledPolygon(polygon, tombe, new IntXY(tileX, tileY), TILING_CONF);
  }

  @Test
  void no_superposition_drops_polygon_mostly_covered_by_a_bigger_one() {
    var big = new LatLonPolygon(square(0, 0, 100, "bati"));
    var coveredSmall = new LatLonPolygon(square(10, 10, 20, "bati"));
    var farSmall = new LatLonPolygon(square(500, 500, 20, "bati"));

    var actual = BoundaryMerger.noSuperposition(Set.of(big, coveredSmall, farSmall), 0.6);

    assertEquals(Set.of(big, farSmall), actual);
  }

  @Test
  void no_superposition_keeps_polygons_overlapping_below_allowed_ratio() {
    var first = new LatLonPolygon(square(0, 0, 100, "bati"));
    var barelyOverlapping = new LatLonPolygon(square(90, 90, 100, "bati"));

    var actual = BoundaryMerger.noSuperposition(Set.of(first, barelyOverlapping), 0.6);

    assertEquals(2, actual.size());
  }

  @Test
  void no_superposition_keeps_polygons_whose_intersection_cannot_be_computed() {
    var small = mock(Polygon.class);
    var big = mock(Polygon.class);
    when(small.getArea()).thenReturn(1.0);
    when(big.getArea()).thenReturn(2.0);
    when(small.intersection(big)).thenThrow(new IllegalStateException("invalid geometry"));
    var smallLatLon = new LatLonPolygon(small);
    var bigLatLon = new LatLonPolygon(big);

    var actual = BoundaryMerger.noSuperposition(Set.of(smallLatLon, bigLatLon), 0.6);

    assertEquals(2, actual.size());
  }

  @Test
  void invert_swaps_x_and_y_of_shell_and_holes_and_keeps_user_data() {
    var shell = FACTORY.createLinearRing(coordinates(0, 0, 10, 0, 10, 20, 0, 20, 0, 0));
    var hole = FACTORY.createLinearRing(coordinates(2, 2, 4, 2, 4, 6, 2, 6, 2, 2));
    var polygon = FACTORY.createPolygon(shell, new LinearRing[] {hole});
    polygon.setUserData(Map.of("label", "bati"));

    var actual = BoundaryMerger.invert(Set.of(new LatLonPolygon(polygon))).iterator().next();

    var inverted = actual.polygon();
    assertArrayEquals(
        coordinates(0, 0, 0, 10, 20, 10, 20, 0, 0, 0), inverted.getExteriorRing().getCoordinates());
    assertArrayEquals(
        coordinates(2, 2, 2, 4, 6, 4, 6, 2, 2, 2), inverted.getInteriorRingN(0).getCoordinates());
    assertEquals(Map.of("label", "bati"), inverted.getUserData());
  }

  @Test
  void invert_closes_an_open_exterior_ring() {
    var openRing = mock(LinearRing.class);
    when(openRing.getCoordinates()).thenReturn(coordinates(0, 0, 5, 0, 5, 5));
    var source = mock(Polygon.class);
    when(source.getExteriorRing()).thenReturn(openRing);
    when(source.getNumInteriorRing()).thenReturn(0);

    var actual = BoundaryMerger.invert(Set.of(new LatLonPolygon(source))).iterator().next();

    assertArrayEquals(
        coordinates(0, 0, 0, 5, 5, 5, 0, 0), actual.polygon().getExteriorRing().getCoordinates());
  }

  @Test
  void with_offset_translates_polygon_by_tile_distance_times_image_size() {
    var polygon = square(10, 20, 100, "bati");
    var tiled = new TiledPolygon(polygon, tombe, new IntXY(3, 2), TILING_CONF);

    var actual = BoundaryMerger.withOffset(tiled, new IntXY(1, 1), TILING_CONF);

    assertArrayEquals(
        coordinates(2_058, 1_044, 2_158, 1_044, 2_158, 1_144, 2_058, 1_144, 2_058, 1_044),
        actual.polygon().getCoordinates());
    assertEquals(Map.of("label", "bati"), actual.polygon().getUserData());
    assertEquals(new IntXY(3, 2), actual.originTile());
  }

  @Test
  void with_offset_closes_an_open_ring_before_translating() {
    var openPolygon = mock(Polygon.class);
    when(openPolygon.getCoordinates()).thenReturn(coordinates(0, 0, 10, 0, 10, 10, 0, 10));
    var tiled = new TiledPolygon(openPolygon, tombe, new IntXY(1, 0), TILING_CONF);

    var actual = BoundaryMerger.withOffset(tiled, new IntXY(0, 0), TILING_CONF);

    assertArrayEquals(
        coordinates(1_024, 0, 1_034, 0, 1_034, 10, 1_024, 10, 1_024, 0),
        actual.polygon().getCoordinates());
  }

  @Test
  void apply_without_polygons_returns_nothing() {
    assertTrue(subject.apply(Set.of(), TOMBE).isEmpty());
  }

  @Test
  void apply_merges_small_neighbouring_tombs_from_adjacent_tiles_into_one_polygon() {
    var left = tombAt(square(0, 0, 60, "tombe"), 0, 0);
    var right = tombAt(square(60, 0, 60, "tombe"), 1, 0);

    var actual = subject.apply(Set.of(left, right), TOMBE);

    assertEquals(1, actual.size());
    var userData = (Map<?, ?>) actual.iterator().next().polygon().getUserData();
    assertEquals("tombe", userData.get("label"));
  }

  @Test
  void apply_keeps_isolated_tombs_as_they_are() {
    var first = tombAt(square(0, 0, 60, "tombe"), 0, 0);
    var second = tombAt(square(900, 900, 60, "tombe"), 0, 0);

    var actual = subject.apply(Set.of(first, second), PASSAGE_PIETON);

    assertEquals(2, actual.size());
  }

  @Test
  void apply_with_offset_aligns_polygons_on_the_first_tile_before_merging() {
    var withOffset = new BoundaryMerger(4_000, 41, true);
    var left = tombAt(square(964, 0, 60, "tombe"), 0, 0);
    var right = tombAt(square(0, 0, 60, "tombe"), 1, 0);

    var actual = withOffset.apply(Set.of(left, right), TOMBE);

    assertEquals(1, actual.size());
  }

  private static Coordinate[] coordinates(double... xy) {
    var coordinates = new Coordinate[xy.length / 2];
    for (int i = 0; i < coordinates.length; i++) {
      coordinates[i] = new Coordinate(xy[2 * i], xy[2 * i + 1]);
    }
    return coordinates;
  }
}
