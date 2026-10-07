package app.bpartners.geojobs.model.lidar.planes.postprocessing;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;

class PanelVerticesMergerTest {
  private static final double TOLERANCE = 0.5;
  private final PanelVerticesMerger subject =
      new PanelVerticesMerger(TOLERANCE, Coordinate::distance);

  @Test
  void close_vertices_of_neighbour_panels_are_replaced_by_the_same_point() {
    var left = polygon(0, 0, 10, 0, 10, 10, 0, 10);
    var right = polygon(10.2, 0, 20, 0, 20, 10, 10.2, 10);

    var merged = subject.apply(List.of(left, right));

    assertEquals(new Coordinate(10.1, 0), merged.get(0)[1]);
    assertEquals(new Coordinate(10.1, 0), merged.get(1)[0]);
    assertEquals(new Coordinate(10.1, 10), merged.get(0)[2]);
    assertEquals(new Coordinate(10.1, 10), merged.get(1)[3]);
  }

  @Test
  void far_vertices_are_kept_untouched() {
    var left = polygon(0, 0, 10, 0, 10, 10, 0, 10);
    var right = polygon(10.8, 0, 20, 0, 20, 10, 10.8, 10);

    var merged = subject.apply(List.of(left, right));

    assertEquals(new Coordinate(10, 0), merged.get(0)[1]);
    assertEquals(new Coordinate(10.8, 0), merged.get(1)[0]);
  }

  @Test
  void vertices_of_the_same_panel_are_never_merged() {
    var narrow = polygon(0, 0, 0.3, 0, 0.3, 10, 0, 10);
    var other = polygon(0.05, -0.1, 5, -5, 5, -10);

    var merged = subject.apply(List.of(narrow, other));

    assertEquals(new Coordinate(0.025, -0.05), merged.get(0)[0]);
    assertEquals(new Coordinate(0.025, -0.05), merged.get(1)[0]);
    assertEquals(new Coordinate(0.3, 0), merged.get(0)[1]);
  }

  @Test
  void a_vertex_is_merged_with_its_closest_neighbour_only() {
    var a = polygon(0, 0, 10, 0, 10, 10);
    var b = polygon(0.1, 0, -10, 0, -10, -10);
    var c = polygon(0.55, 0, 5, -5, 5, -10);

    var merged = subject.apply(List.of(a, b, c));

    assertEquals(new Coordinate(0.05, 0), merged.get(0)[0]);
    assertEquals(new Coordinate(0.05, 0), merged.get(1)[0]);
    assertEquals(new Coordinate(0.55, 0), merged.get(2)[0]);
  }

  @Test
  void same_panels_always_give_the_same_output_whatever_their_order() {
    var a = polygon(0, 0, 10, 0, 10, 10);
    var b = polygon(0.2, 0, -10, 0, -10, -10);
    var c = polygon(0, 0.2, 5, -5, 5, -10);
    var d = polygon(-0.2, 0, -5, 5, -10, 10);

    var merged = subject.apply(List.of(a, b, c, d));
    var mergedAgain = subject.apply(List.of(a, b, c, d));
    var mergedReversed = subject.apply(List.of(d, c, b, a));

    for (int i = 0; i < 4; i++) {
      assertArrayEquals(merged.get(i), mergedAgain.get(i));
      assertArrayEquals(merged.get(i), mergedReversed.get(3 - i));
    }
  }

  @Test
  void short_edges_are_never_merged_crosswise() {
    var left = polygon(0, 0, 6, 0, 6, 3, 6, 3.02, 6, 6, 0, 6);
    var right = polygon(6.05, 0, 12, 0, 12, 6, 6.05, 6, 6.005, 3.035, 6.005, 3.015);

    var merged = subject.apply(List.of(left, right));

    assertEquals(merged.get(0)[3], merged.get(1)[5]);
    assertEquals(new Coordinate(6, 3), merged.get(0)[2]);
    assertEquals(new Coordinate(6.005, 3.035), merged.get(1)[4]);
    assertNotEquals(merged.get(0)[2], merged.get(0)[3]);
    assertNotEquals(merged.get(1)[4], merged.get(1)[5]);
  }

  @Test
  void short_edges_of_panels_with_opposite_orientations_are_never_merged_crosswise() {
    var counterClockwise = polygon(0, 0, 6, 0, 6, 3, 6, 3.02, 6, 6, 0, 6);
    var clockwise = polygon(6.05, 0, 6.005, 3.015, 6.005, 3.035, 6.05, 6, 12, 6, 12, 0);

    var merged = subject.apply(List.of(counterClockwise, clockwise));

    assertEquals(merged.get(0)[3], merged.get(1)[1]);
    assertEquals(new Coordinate(6, 3), merged.get(0)[2]);
    assertEquals(new Coordinate(6.005, 3.035), merged.get(1)[2]);
  }

  @Test
  void empty_panels_are_ignored() {
    var empty = geometryFactory.createPolygon();
    var square = polygon(0, 0, 1, 0, 1, 1, 0, 1);

    var merged = subject.apply(List.of(empty, square));

    assertEquals(0, merged.get(0).length);
    assertEquals(new Coordinate(1, 1), merged.get(1)[2]);
  }

  @Test
  void panels_with_non_finite_coordinates_are_not_merged() {
    var broken = polygon(0, 0, Double.NaN, 0, 1, 1);
    var square = polygon(0.1, 0, 1, 0, 1, 1, 0, 1);

    var merged = subject.apply(List.of(broken, square));

    assertEquals(new Coordinate(0, 0), merged.get(0)[0]);
    assertEquals(new Coordinate(0.1, 0), merged.get(1)[0]);
  }

  @Test
  void original_panel_is_kept_when_the_merged_ring_is_not_valid() {
    var original = polygon(0, 0, 1, 0, 1, 1, 0, 1);

    assertSame(
        original,
        PanelVerticesMerger.withExteriorRing(
            original,
            new Coordinate[] {
              new Coordinate(0, 0), new Coordinate(1, 1), new Coordinate(1, 0), new Coordinate(0, 1)
            }));
    assertSame(
        original,
        PanelVerticesMerger.withExteriorRing(
            original, new Coordinate[] {new Coordinate(0, 0), new Coordinate(1, 0)}));
    assertSame(
        original,
        PanelVerticesMerger.withExteriorRing(
            original,
            new Coordinate[] {
              new Coordinate(0, 0, 4),
              new Coordinate(1, 0, 4),
              new Coordinate(1, 1, 4),
              new Coordinate(1, 1, 6),
              new Coordinate(0, 1, 4)
            }));
    assertSame(
        original,
        PanelVerticesMerger.withExteriorRing(
            original,
            new Coordinate[] {
              new Coordinate(0, 0), new Coordinate(Double.NaN, 0), new Coordinate(1, 1)
            }));
  }

  @Test
  void holes_of_the_panel_are_kept() {
    var shell = polygon(0, 0, 10, 0, 10, 10, 0, 10).getExteriorRing();
    var hole = polygon(4, 4, 6, 4, 6, 6, 4, 6).getExteriorRing();
    var original = geometryFactory.createPolygon(shell, new LinearRing[] {hole});

    var result =
        PanelVerticesMerger.withExteriorRing(
            original,
            new Coordinate[] {
              new Coordinate(0, 0),
              new Coordinate(10.1, 0),
              new Coordinate(10.1, 10),
              new Coordinate(0, 10)
            });

    assertEquals(1, result.getNumInteriorRing());
    assertEquals(new Coordinate(10.1, 0), result.getExteriorRing().getCoordinateN(1));
  }

  private static Polygon polygon(double... xy) {
    var coordinates = new Coordinate[xy.length / 2 + 1];
    for (int i = 0; i < xy.length / 2; i++) {
      coordinates[i] = new Coordinate(xy[2 * i], xy[2 * i + 1]);
    }
    coordinates[xy.length / 2] = coordinates[0];
    return geometryFactory.createPolygon(coordinates);
  }
}
