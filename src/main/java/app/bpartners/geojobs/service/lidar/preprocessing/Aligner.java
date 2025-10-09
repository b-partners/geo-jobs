package app.bpartners.geojobs.service.lidar.preprocessing;

import app.bpartners.geojobs.service.lidar.model.geometry.Axis;
import app.bpartners.geojobs.service.lidar.model.geometry.LasPointGeometry;
import java.util.Collection;
import java.util.function.Function;

public record Aligner(Axis axis)
    implements Function<Collection<LasPointGeometry>, Collection<LasPointGeometry>> {
  @Override
  public Collection<LasPointGeometry> apply(Collection<LasPointGeometry> points) {
    double min = Double.POSITIVE_INFINITY;
    double max = Double.NEGATIVE_INFINITY;
    for (var point : points) {
      double coordinate = point.getCoordinate(axis);
      if (coordinate < min) min = coordinate;
      if (coordinate > max) max = coordinate;
    }

    double mid = (min + max) / 2;
    return points.stream()
        .map(
            point ->
                new LasPointGeometry(
                    mid,
                    point.getCoordinate().getY(),
                    point.getCoordinate().getZ(),
                    point.getClassification()))
        .toList();
  }
}
