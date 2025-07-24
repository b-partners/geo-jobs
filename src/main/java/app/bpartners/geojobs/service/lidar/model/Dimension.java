package app.bpartners.geojobs.service.lidar.model;

import java.util.Comparator;

public record Dimension(Roof roof, Sol sol) {
  private static double round2(double value) {
    return Math.ceil(value * 100) / 100.0;
  }

  public double getSlopeInDegrees() {
    var minZPoint =
        roof.points().stream()
            .min(Comparator.comparingDouble(p -> p.getCoordinate().getZ()))
            .orElseThrow();

    var maxZPoint =
        roof.points().stream()
            .max(Comparator.comparingDouble(p -> p.getCoordinate().getZ()))
            .orElseThrow();

    double dx = maxZPoint.getCoordinate().getX() - minZPoint.getCoordinate().getX();
    double dy = maxZPoint.getCoordinate().getY() - minZPoint.getCoordinate().getY();
    double dz = maxZPoint.getCoordinate().getZ() - minZPoint.getCoordinate().getZ();
    double distance = Math.sqrt(dx * dx + dy * dy);

    if (distance > 0) {
      return round2(Math.toDegrees(Math.atan(dz / distance)));
    }

    return 0;
  }

  public double getHeightInMeters() {
    var minZPoint =
        roof.points().stream()
            .min(Comparator.comparingDouble(a -> a.getCoordinate().getZ()))
            .orElseThrow();

    double meanSolZ =
        sol.points().stream().mapToDouble(p -> p.getCoordinate().getZ()).average().orElseThrow();

    return round2(minZPoint.getCoordinate().getZ() - meanSolZ);
  }
}
