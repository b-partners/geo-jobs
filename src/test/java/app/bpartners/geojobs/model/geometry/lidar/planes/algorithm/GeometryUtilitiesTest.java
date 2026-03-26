package app.bpartners.geojobs.model.geometry.lidar.planes.algorithm;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.model.lidar.planes.algorithm.GeometryUtilities.getLargestPolygon;
import static app.bpartners.geojobs.model.lidar.planes.algorithm.GeometryUtilities.isCompact;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.*;

class GeometryUtilitiesTest {
  private Polygon polygon(double[][] coords) {
    var coordinates = new Coordinate[coords.length + 1];
    for (int i = 0; i < coords.length; i++) {
      coordinates[i] = new Coordinate(coords[i][0], coords[i][1]);
    }
    coordinates[coords.length] = coordinates[0];

    return geometryFactory.createPolygon(coordinates);
  }

  @Test
  void square_should_be_compact() {
    var square =
        polygon(
            new double[][] {
              {0, 0},
              {1, 0},
              {1, 1},
              {0, 1}
            });

    assertTrue(isCompact(square, 0.1));
  }

  @Test
  void thin_rectangle_should_not_be_compact() {
    var thin =
        polygon(
            new double[][] {
              {0, 0},
              {10, 0},
              {10, 0.1},
              {0, 0.1}
            });

    assertFalse(isCompact(thin, 0.1));
  }

  @Test
  void normal_rectangle_should_be_compact() {
    var rect =
        polygon(
            new double[][] {
              {0, 0},
              {4, 0},
              {4, 2},
              {0, 2}
            });

    assertTrue(isCompact(rect, 0.1));
  }

  @Test
  void getLargestPolygon_with_single_polygon_returns_self() {
    var poly = polygon(new double[][] {{0, 0}, {10, 0}, {10, 10}, {0, 10}});

    var result = getLargestPolygon(poly);

    assertEquals(poly, result);
    assertEquals(100.0, result.getArea());
  }

  @Test
  void getLargestPolygon_with_multipolygon_returns_largest_area() {
    var small = polygon(new double[][] {{0, 0}, {2, 0}, {2, 2}, {0, 2}}); // Area 4
    var large = polygon(new double[][] {{10, 10}, {15, 10}, {15, 15}, {10, 15}}); // Area 25

    var multi = geometryFactory.createMultiPolygon(new Polygon[] {small, large});

    var result = getLargestPolygon(multi);

    assertEquals(large, result);
    assertEquals(25.0, result.getArea());
  }

  @Test
  void getLargestPolygon_throws_exception_for_point() {
    var point = geometryFactory.createPoint(new Coordinate(1, 1));

    var exception = assertThrows(IllegalArgumentException.class, () -> getLargestPolygon(point));
    assertTrue(exception.getMessage().contains("Point"));
  }
}
