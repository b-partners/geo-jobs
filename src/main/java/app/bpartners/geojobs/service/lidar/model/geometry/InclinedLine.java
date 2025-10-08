package app.bpartners.geojobs.service.lidar.model.geometry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public record InclinedLine(List<LasPointGeometry> points) {
  public static InclinedLine empty() {
    return new InclinedLine(new ArrayList<>());
  }

  public ZVariation variation(double epsilonZ) {
    return ZVariation.from(points.getFirst(), points.getLast(), epsilonZ);
  }

  public double slope() {
    if (points.size() < 2) {
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

    return this.isNearByX(other, epsilonX)
        && this.isNearByY(other, epsilonY)
        && this.isNearByZ(other, epsilonZ);
  }

  public boolean isNearByX(InclinedLine other, double epsilonX) {
    var thisPointsSortedByX =
        points.stream().sorted(Comparator.comparing(LasPointGeometry::getX)).toList();
    var otherPointsSortedByX =
        other.points().stream().sorted(Comparator.comparing(LasPointGeometry::getX)).toList();

    var thisMinXPoint = thisPointsSortedByX.getFirst();
    var thisMaxXPoint = thisPointsSortedByX.getLast();

    var otherMinXPoint = otherPointsSortedByX.getFirst();
    var otherMaxXPoint = otherPointsSortedByX.getLast();

    return thisMaxXPoint.isNearByX(otherMinXPoint, epsilonX)
        || thisMaxXPoint.isNearByX(otherMaxXPoint, epsilonX)
        || thisMinXPoint.isNearByX(otherMaxXPoint, epsilonX)
        || thisMinXPoint.isNearByX(otherMinXPoint, epsilonX);
  }

  public boolean isNearByY(InclinedLine other, double epsilonY) {
    var thisPointsSortedByY =
        points.stream().sorted(Comparator.comparing(LasPointGeometry::getY)).toList();
    var otherPointsSortedByY =
        other.points().stream().sorted(Comparator.comparing(LasPointGeometry::getY)).toList();

    var thisMinYPoint = thisPointsSortedByY.getFirst();
    var thisMaxYPoint = thisPointsSortedByY.getLast();

    var otherMinYPoint = otherPointsSortedByY.getFirst();
    var otherMaxYPoint = otherPointsSortedByY.getLast();

    return thisMaxYPoint.isNearByY(otherMinYPoint, epsilonY)
        || thisMaxYPoint.isNearByY(otherMaxYPoint, epsilonY)
        || thisMinYPoint.isNearByY(otherMaxYPoint, epsilonY)
        || thisMinYPoint.isNearByY(otherMinYPoint, epsilonY);
  }

  public boolean isNearByZ(InclinedLine other, double epsilonZ) {
    var thisPointsSortedByZ =
        points.stream().sorted(Comparator.comparing(p -> p.getCoordinate().getZ())).toList();
    var otherPointsSortedByZ =
        other.points().stream()
            .sorted(Comparator.comparing(p -> p.getCoordinate().getZ()))
            .toList();

    var thisMinZPoint = thisPointsSortedByZ.getFirst();
    var thisMaxZPoint = thisPointsSortedByZ.getLast();

    var otherMinZPoint = otherPointsSortedByZ.getFirst();
    var otherMaxZPoint = otherPointsSortedByZ.getLast();

    return thisMaxZPoint.isNearByZ(otherMinZPoint, epsilonZ)
        || thisMaxZPoint.isNearByZ(otherMaxZPoint, epsilonZ)
        || thisMinZPoint.isNearByZ(otherMaxZPoint, epsilonZ)
        || thisMinZPoint.isNearByZ(otherMinZPoint, epsilonZ);
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

        if (!inclinedLine.points().getLast().isNearByY(currentPoint, epsilonY)) {
          notUsedPoints.add(currentPoint);
          continue;
        }

        if (!lastValidPoint.isNearByZ(currentPoint, epsilonZ)) {
          notUsedPoints.add(currentPoint);
          continue;
        }

        if (inclinedLine.points().size() == 1) {
          zVariation = ZVariation.from(lastValidPoint, currentPoint, epsilonZ);
          lastValidPoint = currentPoint;
          inclinedLine.points().add(currentPoint);
          continue;
        }

        var newZVariation = ZVariation.from(lastValidPoint, currentPoint, epsilonZ);
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
      if (b.getCoordinate().getZ() > a.getCoordinate().getZ()) {
        return RISING;
      }

      if (a.getCoordinate().getZ() > b.getCoordinate().getZ()) {
        return FALLING;
      }

      return NONE;
    }
  }
}
