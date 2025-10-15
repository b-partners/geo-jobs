package app.bpartners.geojobs.service.lidar.preprocessing;

import static app.bpartners.geojobs.service.lidar.model.geometry.Axis.*;

import app.bpartners.geojobs.service.lidar.model.geometry.Axis;
import app.bpartners.geojobs.service.lidar.model.geometry.LasPointGeometry;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

public record DuplicatePointsOnTwoAxesCleaner(
    Axis axis1,
    Axis axis2,
    double axis1Epsilon,
    double axis2Epsilon,
    BiFunction<LasPointGeometry, LasPointGeometry, LasPointGeometry> computeNewValue)
    implements Function<Collection<LasPointGeometry>, Collection<LasPointGeometry>> {
  @Override
  public Collection<LasPointGeometry> apply(Collection<LasPointGeometry> points) {
    Map<String, LasPointGeometry> map = new HashMap<>();

    for (var p : points) {
      double coordinate1 = p.getCoordinate(axis1);
      double coordinate2 = p.getCoordinate(axis2);

      long coordinate1Key = Math.round(coordinate1 / axis1Epsilon);
      long coordinate2Key = Math.round(coordinate2 / axis2Epsilon);

      var key = String.format("%s_%s", coordinate1Key, coordinate2Key);
      map.compute(
          key,
          (ignored, existing) -> {
            if (existing == null) {
              return p;
            }

            return computeNewValue.apply(existing, p);
          });
    }

    return map.values();
  }

  public static DuplicatePointsOnTwoAxesCleaner xyKeepHighest(double epsilonX, double epsilonY) {
    return new DuplicatePointsOnTwoAxesCleaner(
        X,
        Y,
        epsilonX,
        epsilonY,
        (existing, current) ->
            existing.getCoordinate(Z) > current.getCoordinate(Z) ? existing : current);
  }

  public static DuplicatePointsOnTwoAxesCleaner zyKeepBottom(double epsilonZ, double epsilonY) {
    return new DuplicatePointsOnTwoAxesCleaner(
        Z, Y, epsilonZ, epsilonY, (existing, actual) -> existing.getCoordinate().getZ() > actual.getCoordinate().getZ() ? actual : existing);
  }
}
