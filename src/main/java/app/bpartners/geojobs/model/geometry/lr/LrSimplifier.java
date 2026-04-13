package app.bpartners.geojobs.model.geometry.lr;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;

import java.util.ArrayList;
import java.util.Arrays;
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

    List<Coordinate> simplified = new ArrayList<>();
    int i = 0;
    while (i < n) {
      var start = coordinates.get(i);
      simplified.add(start);

      if (i >= n - 1) {
        break;
      }

      var next = coordinates.get(i + 1);
      var constraint = new LinearStretchConstraint(start, next, angleThreshold);
      i = getNextStretchPointIndex(coordinates, constraint, i + 2);
    }

    return toPolygon(simplified);
  }

  private int getNextStretchPointIndex(List<Coordinate> coordinates, LinearStretchConstraint constraint, int start) {
    int j = start;
    int n = coordinates.size();
    while (j < n && constraint.isAligned(coordinates.get(j))) {
      j++;
    }
    return j;
  }

  private static List<Coordinate> getNotClosedCoordinates(Polygon polygon) {
    var coordinates = polygon.getCoordinates();
    return Arrays.asList(Arrays.copyOf(coordinates, coordinates.length - 1));
  }

  private static Polygon toPolygon(List<Coordinate> coordinates) {
    var result = new ArrayList<>(coordinates);
    result.add(coordinates.getFirst());
    return geometryFactory.createPolygon(result.toArray(Coordinate[]::new));
  }
}
