package app.bpartners.geojobs.service.lidar.model;

import java.util.Comparator;

public record Dimension(Roof roof, Sol sol) {
  private static double round2(double value) {
    return Math.ceil(value * 100) / 100.0;
  }

  public double getSlopeInDegrees() {
    if (hasInvalidPointCount()) {
      return 0;
    }

    double minZ = Double.MAX_VALUE;
    double maxZ = Double.MIN_VALUE;
    LasPointGeometry minZPoint = null;
    LasPointGeometry maxZPoint = null;

    for (var p : roof.points()) {
      double z = p.getCoordinate().getZ();

      if (z < minZ) {
        minZ = z;
        minZPoint = p;
      }

      if (z > maxZ) {
        maxZ = z;
        maxZPoint = p;
      }
    }

    if (minZPoint == null || maxZPoint == null) {
      throw new IllegalStateException("No points found in roof");
    }

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
    if (hasInvalidPointCount()) {
      return 0;
    }

    var minZPoint =
        roof.points().stream()
            .min(Comparator.comparingDouble(p -> p.getCoordinate().getZ()))
            .orElseThrow();

    var meanSolZ =
        sol.points().stream().mapToDouble(p -> p.getCoordinate().getZ()).average().orElseThrow();

    return round2(minZPoint.getCoordinate().getZ() - meanSolZ);
  }

  private boolean hasInvalidPointCount() {
    return roof.points().size() < 2 || sol.points().size() < 2;
  }
}
