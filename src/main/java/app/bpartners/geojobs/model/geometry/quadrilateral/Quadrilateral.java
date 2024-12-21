package app.bpartners.geojobs.model.geometry.quadrilateral;

import static java.util.stream.Collectors.toSet;

import java.util.Arrays;
import java.util.Set;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.locationtech.jts.algorithm.ConvexHull;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

/*
 * Edges are named in a circular direction, that is clockwise or anti-clockwise.
 *
 *   ^ y
 *   |
 *   |    a---------d
 *   |    |         |
 *   |    b---------c
 *   _______________________________> x
 */
@Accessors(fluent = true)
@Getter
public class Quadrilateral {
  private final Coordinate a;
  private final Coordinate b;
  private final Coordinate c;
  private final Coordinate d;
  private final Geometry convexHull;

  public Quadrilateral(Set<Coordinate> coordinates) {
    checkQuadrilateral(coordinates);
    this.convexHull =
        new ConvexHull(coordinates.toArray(new Coordinate[0]), new GeometryFactory())
            .getConvexHull()
            .convexHull();
    this.a = convexHull.getCoordinates()[0];
    this.b = convexHull.getCoordinates()[1];
    this.c = convexHull.getCoordinates()[2];
    this.d = convexHull.getCoordinates()[3];
  }

  public Quadrilateral(Polygon obb) {
    this(Arrays.stream(obb.getCoordinates()).collect(toSet()));
  }

  private void checkQuadrilateral(Set<Coordinate> coordinates) {
    if (coordinates.size() != 4) {
      throw new IllegalArgumentException(
          "Quadrilateral expects 4 coordinates but got: " + coordinates);
    }
  }
}
