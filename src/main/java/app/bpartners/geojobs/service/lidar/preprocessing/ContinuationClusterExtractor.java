package app.bpartners.geojobs.service.lidar.preprocessing;

import app.bpartners.geojobs.service.lidar.model.geometry.Axis;
import app.bpartners.geojobs.service.lidar.model.geometry.LasPointGeometry;
import java.util.*;
import java.util.function.Function;

public record ContinuationClusterExtractor(Axis axis, double discontinuityThreshold)  implements Function<Collection<LasPointGeometry>, List<List<LasPointGeometry>>> {
  @Override
  public List<List<LasPointGeometry>> apply(Collection<LasPointGeometry> points) {
    List<List<LasPointGeometry>> clusters = new ArrayList<>();
    List<LasPointGeometry> currentCluster = new ArrayList<>();

    for (var currentPoint : sortedByAxis(axis, points)) {
      if (currentCluster.isEmpty()) {
        currentCluster.add(currentPoint);
        continue;
      }

      var prevPoint = currentCluster.getLast();
      var diff = Math.abs(currentPoint.getCoordinate(axis) - prevPoint.getCoordinate(axis));
      if (diff > discontinuityThreshold) {
        clusters.add(currentCluster);
        currentCluster = new ArrayList<>();
      }

      currentCluster.add(currentPoint);
    }

    if (!currentCluster.isEmpty()) {
      clusters.add(currentCluster);
    }

    return clusters;
  }

  private static List<LasPointGeometry> sortedByAxis(Axis axis, Collection<LasPointGeometry> points) {
    return new HashSet<>(points)
        .stream().sorted(Comparator.comparingDouble(p -> p.getCoordinate(axis))).toList();
  }
}
