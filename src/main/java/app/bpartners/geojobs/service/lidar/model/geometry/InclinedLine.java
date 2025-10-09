package app.bpartners.geojobs.service.lidar.model.geometry;

import static app.bpartners.geojobs.service.lidar.model.geometry.Axis.*;

import java.util.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public record InclinedLine(List<LasPointGeometry> points) {
  private static final short MINIMUM_LINE_POINT_COUNT = 2;
  private static final float MERGE_LINE_Z_VARIATION = 0.5f;
  private static final float MERGE_POINT_Z_VARIATION = 0;

  public static InclinedLine empty() {
    return new InclinedLine(new ArrayList<>());
  }

  public boolean hasInvalidPointsCount() {
    return points.size() <= MINIMUM_LINE_POINT_COUNT;
  }

  public double slope() {
    if (hasInvalidPointsCount()) {
      return 0.0;
    }

    var first = points.getFirst();
    var last = points.getLast();

    double deltaZ = last.getCoordinate().getZ() - first.getCoordinate().getZ();
    double deltaY = last.getY() - first.getY();

    if (deltaY == 0) {
      return deltaZ > 0 ? 90.0 : (deltaZ < 0 ? -90.0 : 0.0);
    }

    return Math.toDegrees(Math.atan(deltaZ / deltaY));
  }

  public boolean isCompatibleWith(
      InclinedLine other, double dx, double dy, double dz, double epsilonSlope) {
    // Check if both lines have the same variation (/ or \)
    if (!this.variation().equals(other.variation())) {
      return false;
    }

    // Check if slopes are close enough
    if (Math.abs(this.slope() - other.slope()) > epsilonSlope) {
      return false;
    }

    return this.isNear(other, X, dx) && this.isNear(other, Y, dy) && this.isNear(other, Z, dz);
  }

  public boolean isNear(InclinedLine other, Axis axis, double epsilon) {
    var thisPointsSortedByAxis =
        points.stream().sorted(Comparator.comparing(p -> p.getCoordinate(axis))).toList();
    var otherPointsSortedByAxis =
        other.points().stream().sorted(Comparator.comparing(p -> p.getCoordinate(axis))).toList();

    var thisMinXPoint = thisPointsSortedByAxis.getFirst();
    var thisMaxXPoint = thisPointsSortedByAxis.getLast();

    var otherMinXPoint = otherPointsSortedByAxis.getFirst();
    var otherMaxXPoint = otherPointsSortedByAxis.getLast();

    return thisMaxXPoint.isNear(otherMinXPoint, axis, epsilon)
        || thisMaxXPoint.isNear(otherMaxXPoint, axis, epsilon)
        || thisMinXPoint.isNear(otherMaxXPoint, axis, epsilon)
        || thisMinXPoint.isNear(otherMinXPoint, axis, epsilon);
  }

  public static Collection<InclinedLine> from(
      Collection<LasPointGeometry> points, double dy, double dz) {
    List<InclinedLine> results = new ArrayList<>();
    List<LasPointGeometry> pointsToProcess = new ArrayList<>(sortedByYZ(points));

    while (!pointsToProcess.isEmpty()) {
      InclinedLine inclinedLine = InclinedLine.empty();
      List<LasPointGeometry> notUsedPoints = new ArrayList<>();

      ZVariation zVariation = ZVariation.NONE;
      LasPointGeometry lastValidPoint = pointsToProcess.getFirst();

      for (var currentPoint : pointsToProcess) {
        if (inclinedLine.points().isEmpty()) {
          inclinedLine.points().add(currentPoint);
          continue;
        }

        if (!inclinedLine.points().getLast().isNear(currentPoint, Y, dy)) {
          notUsedPoints.add(currentPoint);
          continue;
        }

        if (!lastValidPoint.isNear(currentPoint, Z, dz)) {
          notUsedPoints.add(currentPoint);
          continue;
        }

        if (inclinedLine.points().size() == 1) {
          zVariation = ZVariation.from(lastValidPoint, currentPoint, MERGE_POINT_Z_VARIATION);
          lastValidPoint = currentPoint;
          inclinedLine.points().add(currentPoint);
          continue;
        }

        var newZVariation = ZVariation.from(lastValidPoint, currentPoint, MERGE_POINT_Z_VARIATION);
        if (zVariation.equals(newZVariation)) {
          lastValidPoint = currentPoint;
        }

        inclinedLine.points().add(currentPoint);
      }

      pointsToProcess = notUsedPoints;
      results.add(inclinedLine);
    }

    return results;
  }

  private static List<LasPointGeometry> sortedByYZ(Collection<LasPointGeometry> points) {
    return points.stream()
        .sorted(
            Comparator.comparing(LasPointGeometry::getY)
                .thenComparing(p -> p.getCoordinate().getZ()))
        .toList();
  }

  private ZVariation variation() {
    return ZVariation.from(points.getFirst(), points.getLast(), MERGE_LINE_Z_VARIATION);
  }

  private enum ZVariation {
    NONE,
    RISING,
    FALLING;

    private static ZVariation from(LasPointGeometry a, LasPointGeometry b, double epsilon) {
      if (a.isNear(b, Z, epsilon)) {
        return NONE;
      }

      if (b.getCoordinate().getZ() > a.getCoordinate().getZ()) {
        return RISING;
      }

      return FALLING;
    }
  }
}
