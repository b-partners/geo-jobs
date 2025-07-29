package app.bpartners.geojobs.service.lidar.model;

import static java.util.Comparator.comparingDouble;

import java.util.List;
import java.util.Set;

public record Dimension(Roof roof, Sol sol) {
  private static final int FRACTION_COUNT = 3;
  private static final int MIN_POINTS_ALLOWED = 5;

  private static double round2(double value) {
    return Math.ceil(value * 100) / 100.0;
  }

  public double getSlopeInDegrees() {
    if (hasInvalidPointCount()) {
      return 0;
    }

    int totalRoofPointsSize = roof.points().size();
    int oneThirdCount = totalRoofPointsSize / FRACTION_COUNT;

    var sortedRoofPointsByZ = sortPointsByZ(roof.points());
    var medianLowZPoint = medianOfSortedPoints(sortedRoofPointsByZ.subList(0, oneThirdCount));
    var medianHighZPoint =
        medianOfSortedPoints(
            sortedRoofPointsByZ.subList(totalRoofPointsSize - oneThirdCount, totalRoofPointsSize));

    double dx = medianHighZPoint.getCoordinate().getX() - medianLowZPoint.getCoordinate().getX();
    double dy = medianHighZPoint.getCoordinate().getY() - medianLowZPoint.getCoordinate().getY();
    double dz = medianHighZPoint.getCoordinate().getZ() - medianLowZPoint.getCoordinate().getZ();
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

    int oneThirdCount = roof.points().size() / FRACTION_COUNT;
    var sortedRoofPointsByZ = sortPointsByZ(roof.points());
    var medianLowZPoint = medianOfSortedPoints(sortedRoofPointsByZ.subList(0, oneThirdCount));

    var meanSolZ =
        sol.points().stream().mapToDouble(p -> p.getCoordinate().getZ()).average().orElseThrow();

    return round2(medianLowZPoint.getCoordinate().getZ() - meanSolZ);
  }

  private boolean hasInvalidPointCount() {
    return roof.points().size() < MIN_POINTS_ALLOWED || sol.points().size() < MIN_POINTS_ALLOWED;
  }

  private static List<LasPointGeometry> sortPointsByZ(Set<LasPointGeometry> points) {
    return points.stream().sorted(comparingDouble(p -> p.getCoordinate().getZ())).toList();
  }

  private static LasPointGeometry medianOfSortedPoints(List<LasPointGeometry> points) {
    int mid = points.size() / 2;
    return points.size() % 2 == 0 ? points.get(mid) : points.get(mid + 1);
  }
}
