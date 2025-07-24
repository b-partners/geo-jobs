package app.bpartners.geojobs.endpoint.rest.postprocessing.continuer.confFactory;

import app.bpartners.geojobs.endpoint.rest.postprocessing.model.LatLonPolygon;
import app.bpartners.geojobs.model.geometry.route.ContinuationConf;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Function;

public class ContinuationConfFactory implements Function<Set<LatLonPolygon>, ContinuationConf> {

  @Override
  public ContinuationConf apply(Set<LatLonPolygon> polygons) {
    double avgEdgeLength = averageEdgeLength(polygons);
    double distanceThreshold = avgEdgeLength * 2.5d;

    double angleVariance = computeAngleVariance(polygons);
    double minDirectionThreshold = Math.max(Math.PI / 36, angleVariance * 0.75d);
    double maxDirectionThreshold = Math.min(Math.PI / 2, minDirectionThreshold * 3);

    return new ContinuationConf(minDirectionThreshold, maxDirectionThreshold, distanceThreshold);
  }

  private static double averageEdgeLength(Set<LatLonPolygon> polygons) {
    return polygons.stream()
        .mapToDouble(ContinuationConfFactory::averageEdgeLength)
        .average()
        .orElse(50d);
  }

  private static double averageEdgeLength(LatLonPolygon polygon) {
    var coords = polygon.polygon().getCoordinates();
    double total = 0d;
    for (int i = 0; i < coords.length - 1; i++) {
      total += coords[i].distance(coords[i + 1]);
    }
    return total / (coords.length - 1);
  }

  private static double computeAngleVariance(Set<LatLonPolygon> polygons) {
    var angles =
        polygons.stream().mapToDouble(ContinuationConfFactory::computePolygonMainAngle).toArray();

    double mean = Arrays.stream(angles).average().orElse(0d);
    return Arrays.stream(angles).map(a -> Math.pow(a - mean, 2)).average().orElse(0d);
  }

  private static double computePolygonMainAngle(LatLonPolygon polygon) {
    var coords = polygon.polygon().getCoordinates();
    double maxLength = 0d;
    double angle = 0d;
    for (int i = 0; i < coords.length - 1; i++) {
      var dx = coords[i + 1].x - coords[i].x;
      var dy = coords[i + 1].y - coords[i].y;
      var length = Math.hypot(dx, dy);
      if (length > maxLength) {
        maxLength = length;
        angle = Math.atan2(dy, dx);
      }
    }
    return angle;
  }
}
