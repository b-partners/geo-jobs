package app.bpartners.geojobs.model.geometry.lr;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

@RequiredArgsConstructor
public class LrSimplifier implements Function<Polygon, Polygon> {
  private final double angleThreshold;

  @Override
  public Polygon apply(Polygon polygon) {
    var coordinates = getNotClosedCoordinates(polygon);
    int n = coordinates.size();
    if (n <= 3) return polygon;

    var consumed = new boolean[n];
    var initial = stretchFrom(0, 1, coordinates, consumed, true);
    List<Coordinate> simplified = new ArrayList<>(initial);

    for (int i = 0; i < n; i++) {
      if (consumed[i]) continue;
      int nextIdx = (i + 1) % n;

      if (consumed[nextIdx]) {
        simplified.add(coordinates.get(i));
        consumed[i] = true;
      } else {
        simplified.addAll(stretchFrom(i, nextIdx, coordinates, consumed, false));
      }
    }

    return toPolygon(simplified);
  }

  private List<Coordinate> stretchFrom(
      int i, int j, List<Coordinate> coordinates, boolean[] consumed, boolean bidirectional) {
    var stretchPoint = coordinates.get(i);
    var baseNeighbor = coordinates.get(j);
    var constraint = new LinearStretchConstraint(stretchPoint, baseNeighbor, angleThreshold);
    consumed[i] = true;
    consumed[j] = true;

    stretchForward(constraint, coordinates, consumed, j);
    if (bidirectional) {
      stretchBackward(constraint, coordinates, consumed, i);
    }

    return List.of(constraint.getStart(), constraint.getEnd());
  }

  private void stretchForward(
      LinearStretchConstraint constraint,
      List<Coordinate> coordinates,
      boolean[] consumed,
      int fromIdx) {
    int n = coordinates.size();
    int next = (fromIdx + 1) % n;
    while (!consumed[next] && constraint.addToEnd(coordinates.get(next))) {
      consumed[next] = true;
      next = (next + 1) % n;
    }
  }

  private void stretchBackward(
      LinearStretchConstraint constraint,
      List<Coordinate> coordinates,
      boolean[] consumed,
      int fromIdx) {
    int n = coordinates.size();
    int prev = (fromIdx - 1 + n) % n;
    while (!consumed[prev] && constraint.addToStart(coordinates.get(prev))) {
      consumed[prev] = true;
      prev = (prev - 1 + n) % n;
    }
  }

  private static List<Coordinate> getNotClosedCoordinates(Polygon polygon) {
    var coordinates = polygon.getCoordinates();
    return List.of(coordinates).subList(0, coordinates.length - 1);
  }

  private static Polygon toPolygon(List<Coordinate> coordinates) {
    var result = new ArrayList<>(coordinates);
    result.add(coordinates.getFirst());
    return geometryFactory.createPolygon(result.toArray(Coordinate[]::new));
  }
}
