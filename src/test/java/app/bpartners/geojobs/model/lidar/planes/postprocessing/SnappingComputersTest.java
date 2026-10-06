package app.bpartners.geojobs.model.lidar.planes.postprocessing;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.model.lidar.planes.algorithm.GeometryUtilities.project;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.model.lidar.planes.Plane3D;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;

class SnappingComputersTest {
  private final Snapping2DComputer snapping2D = new Snapping2DComputer(0.5);
  private final Snapping3DComputer snapping3D = new Snapping3DComputer(1);

  @Test
  void random_roofs_always_give_valid_panels_that_do_not_depend_on_the_run_or_the_order() {
    for (int seed = 0; seed < 300; seed++) {
      var planes = randomRoof(new Random(seed));

      var snapped = snap(planes);

      for (var plane : snapped) {
        assertTrue(plane.getDelimitation().isValid(), "seed " + seed);
        var ring = plane.getDelimitation().getExteriorRing().getCoordinates();
        for (int i = 0; i < ring.length - 1; i++) {
          assertTrue(ring[i].distance(ring[i + 1]) > 0, "collapsed edge, seed " + seed);
        }
      }
      assertEquals(rings(snapped), rings(snap(planes)), "seed " + seed);
      assertEquals(rings(snapped), rings(snap(planes.reversed()).reversed()), "seed " + seed);
    }
  }

  @Test
  void short_edges_never_collapse() {
    var left = plane(0, 0, 4, 0, 0, 6, 0, 6, 3, 6, 3.02, 6, 6, 0, 6);
    var right = plane(0, 0, 4, 6.05, 0, 12, 0, 12, 6, 6.05, 6, 6.005, 3.035, 6.005, 3.015);

    var snapped = snap(List.of(left, right));

    for (var plane : snapped) {
      var ring = plane.getDelimitation().getExteriorRing().getCoordinates();
      for (int i = 0; i < ring.length - 1; i++) {
        assertTrue(ring[i].distance(ring[i + 1]) > 0.01);
      }
    }
  }

  @Test
  void no_panel_gives_no_panel() {
    assertEquals(List.of(), snap(List.of()));
  }

  @Test
  void empty_panel_is_kept_as_is() {
    var empty = plane(0, 0, 4).toBuilder().delimitation(geometryFactory.createPolygon()).build();
    var square = plane(0, 0, 4, 0, 0, 1, 0, 1, 1, 0, 1);

    var snapped = snap(List.of(empty, square));

    assertTrue(snapped.getFirst().getDelimitation().isEmpty());
    assertEquals(square.getDelimitation(), snapped.get(1).getDelimitation());
  }

  @Test
  void vertical_panel_is_kept_as_is() {
    var vertical =
        Plane3D.builder()
            .a(1)
            .b(0)
            .c(0)
            .d(-6)
            .points(Set.of())
            .delimitation(plane(0, 0, 4, 6, 0, 6.1, 0, 6.1, 6, 6, 6).getDelimitation())
            .build();
    var square = plane(0, 0, 4, 0, 0, 6.05, 0, 6.05, 6, 0, 6);

    var snapped = snapping2D.apply(List.of(vertical, square));

    assertSame(vertical, snapped.getFirst());
  }

  @Test
  void holes_are_kept() {
    var flat = plane(0, 0, 4);
    var shell = project(flat, square(0, 0, 10)).getExteriorRing();
    var hole = project(flat, square(4, 4, 2)).getExteriorRing();
    var withHole =
        flat.toBuilder()
            .delimitation(geometryFactory.createPolygon(shell, new LinearRing[] {hole}))
            .build();
    var neighbour = plane(0, 0, 4, 10.1, 0, 20, 0, 20, 10, 10.1, 10);

    var snapped = snap(List.of(withHole, neighbour));

    assertEquals(1, snapped.getFirst().getDelimitation().getNumInteriorRing());
    var merged = snapped.getFirst().getDelimitation().getExteriorRing().getCoordinateN(1);
    assertEquals(0, merged.distance(new Coordinate(10.05, 0)), 1e-9);
  }

  private List<Plane3D> snap(List<Plane3D> planes) {
    return snapping3D.apply(snapping2D.apply(planes));
  }

  private static List<String> rings(List<Plane3D> planes) {
    return planes.stream()
        .map(plane -> Arrays.toString(plane.getDelimitation().getCoordinates()))
        .toList();
  }

  /**
   * A grid of panels of random sizes, from 20 cm to 6 m. Each panel has its own noisy copy of the
   * grid corners, may have short edges of a few millimeters to centimeters near its corners, and
   * may be clockwise. Some tiny panels are added at random corners.
   */
  private static List<Plane3D> randomRoof(Random random) {
    int columns = 1 + random.nextInt(4);
    int rows = 1 + random.nextInt(3);
    double[] xs = steps(random, columns);
    double[] ys = steps(random, rows);
    double noise = random.nextDouble() * 0.4;

    var planes = new ArrayList<Plane3D>();
    for (int column = 0; column < columns; column++) {
      for (int row = 0; row < rows; row++) {
        var corners =
            List.of(
                new double[] {xs[column], ys[row]},
                new double[] {xs[column + 1], ys[row]},
                new double[] {xs[column + 1], ys[row + 1]},
                new double[] {xs[column], ys[row + 1]});
        var ring = new ArrayList<Double>();
        for (int i = 0; i < 4; i++) {
          var corner = corners.get(i);
          var next = corners.get((i + 1) % 4);
          double x = corner[0] + (random.nextDouble() * 2 - 1) * noise;
          double y = corner[1] + (random.nextDouble() * 2 - 1) * noise;
          ring.add(x);
          ring.add(y);
          if (random.nextBoolean()) {
            double length = 0.001 + random.nextDouble() * 0.05;
            double edge = Math.hypot(next[0] - corner[0], next[1] - corner[1]);
            ring.add(x + (next[0] - corner[0]) / edge * length);
            ring.add(y + (next[1] - corner[1]) / edge * length);
          }
        }
        addIfValid(planes, random, ring, random.nextBoolean());
      }
    }
    for (int i = random.nextInt(3); i > 0; i--) {
      double x = xs[random.nextInt(xs.length)];
      double y = ys[random.nextInt(ys.length)];
      double size = 0.005 + random.nextDouble() * 0.1;
      addIfValid(
          planes, random, List.of(x, y, x + size, y, x + size, y + size, x, y + size), false);
    }
    return planes;
  }

  private static double[] steps(Random random, int count) {
    var steps = new double[count + 1];
    for (int i = 1; i <= count; i++) {
      steps[i] = steps[i - 1] + 0.2 + random.nextDouble() * 5.8;
    }
    return steps;
  }

  private static void addIfValid(
      List<Plane3D> planes, Random random, List<Double> ring, boolean clockwise) {
    var xy = new double[ring.size()];
    for (int i = 0; i < ring.size(); i += 2) {
      int target = clockwise ? ring.size() - 2 - i : i;
      xy[target] = ring.get(i);
      xy[target + 1] = ring.get(i + 1);
    }
    var plane =
        plane(
            random.nextDouble() * 2 - 1,
            random.nextDouble() * 2 - 1,
            2 + random.nextDouble() * 8,
            xy);
    if (plane.getDelimitation().isValid()) {
      planes.add(plane);
    }
  }

  /** Plane z = sx * x + sy * y + z0, delimited by the given xy vertices projected on it. */
  private static Plane3D plane(double sx, double sy, double z0, double... xy) {
    var plane = Plane3D.builder().a(sx).b(sy).c(-1).d(z0).points(Set.of()).build();
    if (xy.length == 0) {
      return plane;
    }
    var coordinates = new Coordinate[xy.length / 2 + 1];
    for (int i = 0; i < xy.length / 2; i++) {
      coordinates[i] = new Coordinate(xy[2 * i], xy[2 * i + 1]);
    }
    coordinates[xy.length / 2] = coordinates[0].copy();
    return plane.toBuilder()
        .delimitation(project(plane, geometryFactory.createPolygon(coordinates)))
        .build();
  }

  private static Polygon square(double x, double y, double size) {
    return geometryFactory.createPolygon(
        new Coordinate[] {
          new Coordinate(x, y),
          new Coordinate(x + size, y),
          new Coordinate(x + size, y + size),
          new Coordinate(x, y + size),
          new Coordinate(x, y)
        });
  }
}
