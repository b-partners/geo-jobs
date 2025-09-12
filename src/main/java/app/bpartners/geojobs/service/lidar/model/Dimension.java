package app.bpartners.geojobs.service.lidar.model;

import static java.util.Comparator.comparingDouble;

import app.bpartners.geojobs.service.lidar.preprocessing.RoofPointsCleaner;
import app.bpartners.geojobs.service.lidar.preprocessing.SolPointsCleaner;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;

@Slf4j
public record Dimension(Roof roof, Sol sol) {
  private static final int MIN_VALID_POINT_COUNT = 5;
  private static final double LOWEST_Z_RATIO = 0.30;
  private static final double HIGHEST_Z_RATIO = 0.20;

  private static final SolPointsCleaner solPointsCleaner = new SolPointsCleaner();
  private static final RoofPointsCleaner roofPointsCleaner = new RoofPointsCleaner();

  public static Dimension empty() {
    return new Dimension(new Roof(new HashSet<>()), new Sol(new HashSet<>()));
  }

  public double getSlopeInDegrees() {
    if (hasInvalidPointCount()) {
      return 0;
    }

    var sortedRoofPointsByZ = sortedByZ(roofPointsCleaner.compute(roof.points()));
    var lowPoints = getLowerZPoints(sortedRoofPointsByZ);
    var highPoints = getHigherZPoints(sortedRoofPointsByZ);

    var zMin = lowPoints.stream().mapToDouble(p -> p.getCoordinate().getZ()).average().orElse(0);
    var zMax = highPoints.stream().mapToDouble(p -> p.getCoordinate().getZ()).average().orElse(0);

    var lowCentroid = centroidXY(lowPoints);
    var highCentroid = centroidXY(highPoints);

    double dx = highCentroid.getX() - lowCentroid.getX();
    double dy = highCentroid.getY() - lowCentroid.getY();
    double d = Math.sqrt(dx * dx + dy * dy);

    return d > 0 ? ceil2(Math.toDegrees(Math.atan((zMax - zMin) / d))) : 0;
  }

  public double getHeightInMeters() {
    if (hasInvalidPointCount()) {
      return 0;
    }

    var cleanedRoofPoints = roofPointsCleaner.compute(roof.points());
    var sortedRoofPointsByZ = sortedByZ(cleanedRoofPoints);
    var highPoints = getHigherZPoints(sortedRoofPointsByZ);
    double zMax =
        highPoints.stream().mapToDouble(p -> p.getCoordinate().getZ()).average().orElse(0);

    var sortedSolPointsByZ = sortedByZ(solPointsCleaner.compute(sol.points()));
    var meanSolZ =
        sortedSolPointsByZ.stream().mapToDouble(p -> p.getCoordinate().getZ()).average().orElse(0);

    return ceil2(zMax - meanSolZ);
  }

  public boolean hasInvalidPointCount() {
    return roof.points().size() < MIN_VALID_POINT_COUNT
        || sol.points().size() < MIN_VALID_POINT_COUNT;
  }

  private static List<LasPointGeometry> getLowerZPoints(List<LasPointGeometry> sorted) {
    int count = Math.max(1, (int) (sorted.size() * LOWEST_Z_RATIO));
    return sorted.subList(0, Math.min(count, sorted.size()));
  }

  private static List<LasPointGeometry> getHigherZPoints(List<LasPointGeometry> sorted) {
    int count = Math.max(1, (int) (sorted.size() * HIGHEST_Z_RATIO));
    int fromIndex = Math.max(0, sorted.size() - count);
    return sorted.subList(fromIndex, sorted.size());
  }

  private static Coordinate centroidXY(List<LasPointGeometry> points) {
    double sumX = 0;
    double sumY = 0;

    for (var p : points) {
      sumX += p.getCoordinate().getX();
      sumY += p.getCoordinate().getY();
    }
    return new Coordinate(sumX / points.size(), sumY / points.size());
  }

  private static double ceil2(double value) {
    return Math.ceil(value * 100) / 100.0;
  }

  private static List<LasPointGeometry> sortedByZ(Set<LasPointGeometry> points) {
    return points.stream().sorted(comparingDouble(p -> p.getCoordinate().getZ())).toList();
  }
}
