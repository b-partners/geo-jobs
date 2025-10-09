package app.bpartners.geojobs.service.lidar.model.geometry;

import java.util.*;

import lombok.extern.slf4j.Slf4j;

import static app.bpartners.geojobs.service.lidar.model.geometry.Axis.*;

@Slf4j
public record InclinedLine(List<LasPointGeometry> points) {
  private static final short MINIMUM_LINE_POINT_COUNT = 2;

  public static InclinedLine empty() {
    return new InclinedLine(new ArrayList<>());
  }

  public ZVariation variation(double epsilonZ) {
    return ZVariation.from(points.getFirst(), points.getLast(), epsilonZ);
  }

  public double slope() {
    if (points.size() < MINIMUM_LINE_POINT_COUNT) {
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

  public boolean isMergeableWith(
      InclinedLine other, double epsilonX, double epsilonY, double epsilonZ, double epsilonSlope) {
    // Check if both lines have the same variation (/ or \)
    if (!this.variation(epsilonZ).equals(other.variation(epsilonZ))) {
      return false;
    }

    // Check if slopes are close enough
    if (Math.abs(this.slope() - other.slope()) > epsilonSlope) {
      return false;
    }

    var x =  this.isNear(other, X, epsilonX);
    var y = this.isNear(other, Y,  epsilonY);
    var z = this.isNear(other, Z, epsilonZ);

    log.info("x={}, y={}, z={}", x, y, z);
    return x && y && z;
  }

  public boolean isNear(InclinedLine other, Axis axis, double epsilon){
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

  public static List<InclinedLine> from(
      List<LasPointGeometry> points, double epsilonY, double epsilonZ) {
    List<InclinedLine> results = new ArrayList<>();
    List<LasPointGeometry> pointsToProcess = new ArrayList<>(points);

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

        if (!inclinedLine.points().getLast().isNear(currentPoint, Y, epsilonY)) {
          notUsedPoints.add(currentPoint);
          continue;
        }

        if (!lastValidPoint.isNear(currentPoint, Z, epsilonZ)) {
          notUsedPoints.add(currentPoint);
          continue;
        }

        if (inclinedLine.points().size() == 1) {
          zVariation = ZVariation.from(lastValidPoint, currentPoint, 0);
          lastValidPoint = currentPoint;
          inclinedLine.points().add(currentPoint);
          continue;
        }

        var newZVariation = ZVariation.from(lastValidPoint, currentPoint, 0);
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

  public enum ZVariation {
    NONE,
    RISING,
    FALLING;

    public static ZVariation from(LasPointGeometry a, LasPointGeometry b, double epsilon) {
        if(a.isNear(b, Z, epsilon)) {
            return NONE;
        }

        if (b.getCoordinate().getZ() > a.getCoordinate().getZ()) {
            return RISING;
        }

        return FALLING;
    }
  }
}
