package app.bpartners.geojobs.service.lidar.model.geometry;

import java.util.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public record LasPointGroupedByX(List<List<LasPointGeometry>> groups) {
  public static LasPointGroupedByX from(Collection<LasPointGeometry> points, double epsilonX) {
    List<List<LasPointGeometry>> groups = new ArrayList<>();
    List<LasPointGeometry> currentGroup = new ArrayList<>();

    for (var point : sortedByX(points)) {
      if (currentGroup.isEmpty() || currentGroup.getFirst().isNearByX(point, epsilonX)) {
        currentGroup.add(point);
      } else {
        groups.add(currentGroup);
        currentGroup = new ArrayList<>();
        currentGroup.add(point);
      }
    }

    if (!currentGroup.isEmpty()) {
      groups.add(currentGroup);
    }

    return new LasPointGroupedByX(groups);
  }

  private static List<LasPointGeometry> sortedByX(Collection<LasPointGeometry> points) {
    return points.stream().sorted(Comparator.comparing(LasPointGeometry::getX)).toList();
  }
}
