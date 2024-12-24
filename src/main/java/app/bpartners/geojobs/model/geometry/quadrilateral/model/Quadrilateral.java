package app.bpartners.geojobs.model.geometry.quadrilateral.model;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static java.util.Comparator.comparingDouble;
import static java.util.stream.Collectors.toSet;

import app.bpartners.geojobs.model.geometry.polygon.PolygonEdges;
import app.bpartners.geojobs.model.geometry.polygon.PolygonOrientation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.locationtech.jts.algorithm.hull.ConcaveHull;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

/*
 * Vertices are named in a circular direction, that is clockwise or anti-clockwise.
 * Vertex 'a' is the one that is on a length edge, and that is closest to origin.
 *
 *   ^ y
 *   |
 *   |    d---------c
 *   |    |         |
 *   |    a---------b
 *   _______________________________> x
 */
@Accessors(fluent = true)
@Getter
public class Quadrilateral {
  private final Coordinate a;
  private final Coordinate b;
  private final Coordinate c;
  private final Coordinate d;
  private final Polygon polygon;

  public Quadrilateral(Polygon p) {
    this(Arrays.stream(p.getCoordinates()).collect(toSet()));
  }

  public Quadrilateral(Set<Coordinate> coordinates) {
    checkQuadrilateral(coordinates);
    this.polygon =
        // _not_ convex hull as edges will be swallowed by
        // the (over-approximation of the) convex hull if quadrilateral is concave
        (Polygon)
            new ConcaveHull(
                    geometryFactory.createMultiPointFromCoords(
                        coordinates.toArray(new Coordinate[0])))
                .getHull();

    int indexOfLengthEdgeClosestToOrigin = indexOfLengthVerticeClosestToOrigin(polygon);
    var concaveHullCoordinates = polygon.getCoordinates();
    this.a = concaveHullCoordinates[indexOfLengthEdgeClosestToOrigin];
    this.b = concaveHullCoordinates[(indexOfLengthEdgeClosestToOrigin + 1) % 4];
    this.c = concaveHullCoordinates[(indexOfLengthEdgeClosestToOrigin + 2) % 4];
    this.d = concaveHullCoordinates[(indexOfLengthEdgeClosestToOrigin + 3) % 4];
  }

  /**
   * Length vertices are vertices that define the length edges. A quadrilateral has 2 length
   * vertices, and 2 width vertices.
   */
  private int indexOfLengthVerticeClosestToOrigin(Polygon polygon) {
    List<LineSegment> lengthEdges =
        new PolygonEdges(polygon)
            .get().stream().sorted(comparingDouble(LineSegment::getLength)).limit(2).toList();

    var origin = new Coordinate(0, 0);
    var closestToOrigin =
        lengthEdges.stream()
            .flatMap(vertice -> Stream.of(vertice.p0, vertice.p1))
            .min(comparingDouble(p -> p.distance(origin)))
            .get();

    var coordinatesOfRing =
        new ArrayList<>(Arrays.asList(polygon.getExteriorRing().getCoordinates()));
    return coordinatesOfRing.indexOf(closestToOrigin);
  }

  private void checkQuadrilateral(Collection<Coordinate> coordinates) {
    if (coordinates.size() != 4) {
      throw new IllegalArgumentException(
          "Quadrilateral expects 4 coordinates but got: " + coordinates);
    }
  }

  public Point centroid() {
    return polygon.getCentroid();
  }

  public double angle() {
    return new PolygonOrientation(polygon).get();
  }
}
