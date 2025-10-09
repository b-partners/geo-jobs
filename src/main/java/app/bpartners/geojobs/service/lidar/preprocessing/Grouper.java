package app.bpartners.geojobs.service.lidar.preprocessing;

import app.bpartners.geojobs.service.lidar.model.geometry.Axis;
import app.bpartners.geojobs.service.lidar.model.geometry.LasPointGeometry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

public record Grouper(Axis axis, double epsilon)
    implements Function<Collection<LasPointGeometry>, List<List<LasPointGeometry>>> {
  @Override
  public List<List<LasPointGeometry>> apply(Collection<LasPointGeometry> points) {
    List<List<LasPointGeometry>> groups = new ArrayList<>();
    List<LasPointGeometry> currentGroup = new ArrayList<>();

    for (var point : sortedByAxis(points)) {
      if (currentGroup.isEmpty() || currentGroup.getFirst().isNear(point, axis, epsilon)) {
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

    return groups;
  }

  private List<LasPointGeometry> sortedByAxis(Collection<LasPointGeometry> points) {
    return points.stream().sorted(Comparator.comparing(p -> p.getCoordinate(axis))).toList();
  }
}
