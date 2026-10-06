package app.bpartners.geojobs.model.lidar.planes.postprocessing;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static java.util.Comparator.comparingDouble;

import java.util.*;
import java.util.function.ToDoubleBiFunction;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;

/**
 * Merges the vertices of neighbour panels that are closer than the tolerance. A merged vertex is
 * replaced by the centroid of its group, while a vertex without a close neighbour is kept as is.
 * Two vertices of the same panel are never merged together, and every vertex of a group must be
 * within the tolerance of all the others so that groups do not grow by chaining. An edge shared by
 * two panels must be walked in opposite directions by them, so that short edges are never merged
 * crosswise, which would collapse or fold them.
 *
 * <p>The result only depends on the vertices positions: pairs at the same distance are ordered by
 * their coordinates, and centroids are summed in coordinates order, so that the same panels always
 * give the same output whatever their order.
 */
@RequiredArgsConstructor
class PanelVerticesMerger {
  private static final double MIN_EDGE_LENGTH = 1e-6;

  private final double tolerance;
  private final ToDoubleBiFunction<Coordinate, Coordinate> distance;

  /**
   * @return for each panel, the vertices of its exterior ring (without the closing point) at their
   *     merged position, empty for an empty panel
   */
  List<Coordinate[]> apply(List<Polygon> panels) {
    var vertices = toVertices(panels);
    var groupOf = new ArrayList<List<Vertex>>();
    for (var vertex : vertices) {
      groupOf.add(new ArrayList<>(List.of(vertex)));
    }

    for (var pair : closePairsSortedByDistance(vertices)) {
      var first = groupOf.get(pair.first().index());
      var second = groupOf.get(pair.second().index());
      if (first == second || !canMerge(first, second, groupOf)) {
        continue;
      }
      first.addAll(second);
      second.forEach(vertex -> groupOf.set(vertex.index(), first));
    }

    var result = new ArrayList<Coordinate[]>();
    for (var panel : panels) {
      result.add(new Coordinate[ringVertices(panel).length]);
    }
    for (var vertex : vertices) {
      result.get(vertex.panel())[vertex.position()] = centroid(groupOf.get(vertex.index()));
    }
    return result;
  }

  /**
   * @return the panel with the given exterior ring vertices and its original holes, or the
   *     original panel when that would not be a valid polygon or would have a collapsed edge
   */
  static Polygon withExteriorRing(Polygon original, Coordinate[] vertices) {
    if (vertices.length < 3
        || !Arrays.stream(vertices).allMatch(PanelVerticesMerger::isFinite)
        || hasCollapsedEdge(vertices)) {
      return original;
    }
    var holes = new LinearRing[original.getNumInteriorRing()];
    for (int i = 0; i < holes.length; i++) {
      holes[i] = (LinearRing) original.getInteriorRingN(i).copy();
    }
    var polygon = geometryFactory.createPolygon(geometryFactory.createLinearRing(closed(vertices)), holes);
    return polygon.isValid() ? polygon : original;
  }

  private static boolean hasCollapsedEdge(Coordinate[] vertices) {
    for (int i = 0; i < vertices.length; i++) {
      if (vertices[i].distance(vertices[(i + 1) % vertices.length]) < MIN_EDGE_LENGTH) {
        return true;
      }
    }
    return false;
  }

  static Coordinate[] closed(Coordinate[] vertices) {
    var ring = Arrays.copyOf(vertices, vertices.length + 1);
    ring[vertices.length] = ring[0].copy();
    return ring;
  }

  private static List<Vertex> toVertices(List<Polygon> panels) {
    var vertices = new ArrayList<Vertex>();
    for (int panel = 0; panel < panels.size(); panel++) {
      var ring = ringVertices(panels.get(panel));
      if (ring.length < 3 || !Arrays.stream(ring).allMatch(PanelVerticesMerger::isFinite)) {
        for (int position = 0; position < ring.length; position++) {
          vertices.add(new Vertex(vertices.size(), panel, position, ring[position], -1, -1));
        }
        continue;
      }
      // next and previous are given in counter-clockwise order, whatever the ring orientation
      int step = signedArea(ring) >= 0 ? 1 : -1;
      int first = vertices.size();
      for (int position = 0; position < ring.length; position++) {
        int next = first + Math.floorMod(position + step, ring.length);
        int previous = first + Math.floorMod(position - step, ring.length);
        vertices.add(new Vertex(vertices.size(), panel, position, ring[position], next, previous));
      }
    }
    return vertices;
  }

  private static Coordinate[] ringVertices(Polygon panel) {
    var ring = panel.getExteriorRing().getCoordinates();
    return ring.length < 4 ? new Coordinate[0] : Arrays.copyOf(ring, ring.length - 1);
  }

  private static double signedArea(Coordinate[] ring) {
    double area = 0;
    for (int i = 0; i < ring.length; i++) {
      var a = ring[i];
      var b = ring[(i + 1) % ring.length];
      area += a.getX() * b.getY() - b.getX() * a.getY();
    }
    return area;
  }

  private static boolean isFinite(Coordinate coordinate) {
    return Double.isFinite(coordinate.getX()) && Double.isFinite(coordinate.getY());
  }

  private List<VertexPair> closePairsSortedByDistance(List<Vertex> vertices) {
    var pairs = new ArrayList<VertexPair>();
    for (int i = 0; i < vertices.size(); i++) {
      for (int j = i + 1; j < vertices.size(); j++) {
        var first = vertices.get(i);
        var second = vertices.get(j);
        if (first.panel() == second.panel() || first.next() < 0 || second.next() < 0) {
          continue;
        }
        var d = distance.applyAsDouble(first.coordinate(), second.coordinate());
        if (d <= tolerance) {
          pairs.add(VertexPair.of(first, second, d));
        }
      }
    }
    pairs.sort(
        comparingDouble(VertexPair::distance)
            .thenComparing(VertexPair::first, BY_COORDINATE)
            .thenComparing(VertexPair::second, BY_COORDINATE));
    return pairs;
  }

  private boolean canMerge(List<Vertex> first, List<Vertex> second, List<List<Vertex>> groupOf) {
    for (var a : first) {
      for (var b : second) {
        if (a.panel() == b.panel()
            || distance.applyAsDouble(a.coordinate(), b.coordinate()) > tolerance
            || groupOf.get(a.next()) == groupOf.get(b.next())
            || groupOf.get(a.previous()) == groupOf.get(b.previous())) {
          return false;
        }
      }
    }
    return true;
  }

  private static Coordinate centroid(List<Vertex> group) {
    if (group.size() == 1) {
      return group.getFirst().coordinate().copy();
    }
    double x = 0;
    double y = 0;
    double z = 0;
    for (var vertex : group.stream().sorted(BY_COORDINATE).toList()) {
      x += vertex.coordinate().getX();
      y += vertex.coordinate().getY();
      z += vertex.coordinate().getZ();
    }
    return new Coordinate(x / group.size(), y / group.size(), z / group.size());
  }

  /** Index of the next and previous vertices of the panel, -1 when the panel is not merged. */
  private record Vertex(
      int index, int panel, int position, Coordinate coordinate, int next, int previous) {}

  private static final Comparator<Vertex> BY_COORDINATE =
      Comparator.comparing(Vertex::coordinate, Coordinate::compareTo)
          .thenComparingDouble(vertex -> vertex.coordinate().getZ());

  private record VertexPair(Vertex first, Vertex second, double distance) {
    static VertexPair of(Vertex a, Vertex b, double distance) {
      return BY_COORDINATE.compare(a, b) <= 0
          ? new VertexPair(a, b, distance)
          : new VertexPair(b, a, distance);
    }
  }
}
