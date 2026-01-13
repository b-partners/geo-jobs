package app.bpartners.geojobs.model.lidar.planes;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Builder
@RequiredArgsConstructor
public class Kernel {
  private final int attempts;
  private final double threshold;
  private final double minVectorNorm;
  private final double orthogonalDegEpsilon;

  @Getter private final List<LasPointGeometry> points;
  private static final double ORTHOGONAL_ANGLE = 90.0;

  public static Optional<Kernel> from(
      Collection<LasPointGeometry> points,
      SecureRandom random,
      int attempts,
      double threshold,
      double minVectorNorm,
      double orthogonalDegEpsilon) {
    var kernelPoints =
        getOrthogonalTriplet(
            new ArrayList<>(points),
            random,
            attempts,
            threshold,
            minVectorNorm,
            orthogonalDegEpsilon);

    if (kernelPoints.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(
        Kernel.builder()
            .points(kernelPoints)
            .attempts(attempts)
            .threshold(threshold)
            .minVectorNorm(minVectorNorm)
            .orthogonalDegEpsilon(orthogonalDegEpsilon)
            .build());
  }

  private static List<LasPointGeometry> getOrthogonalTriplet(
      List<LasPointGeometry> points,
      SecureRandom random,
      int attempts,
      double threshold,
      double minVectorNorm,
      double orthogonalAngleDegEpsilon) {
    var p1 = points.get(random.nextInt(points.size()));
    var neighbors = getNeighbors(p1, points, threshold);

    if (neighbors.size() < 2) {
      return List.of();
    }

    var maxAbsCosine = getMaxAbsCosine(orthogonalAngleDegEpsilon);
    for (int i = 0; i < attempts; i++) {
      var p2 = neighbors.get(random.nextInt(neighbors.size()));
      if (p2 == p1) continue;

      var v1 = Vector3D.from(p1, p2);
      var n1 = v1.norm();
      if (n1 < minVectorNorm) continue;

      for (var p3 : neighbors) {
        if (p1 == p3 || p2 == p3) continue;

        var v2 = Vector3D.from(p1, p3);
        var n2 = v2.norm();
        if (n2 < minVectorNorm) continue;

        double cos = Math.abs(v1.dot(v2) / (n1 * n2));
        if (cos < maxAbsCosine) {
          return List.of(p1, p2, p3);
        }
      }
    }
    return List.of();
  }

  private static List<LasPointGeometry> getNeighbors(
      LasPointGeometry point, Collection<LasPointGeometry> points, double neighborRadius) {
    return points.stream().filter(p -> p != point && point.distance(p) < neighborRadius).toList();
  }

  private static double getMaxAbsCosine(double orthogonalAngleDegEpsilon) {
    return Math.cos(Math.toRadians(ORTHOGONAL_ANGLE - orthogonalAngleDegEpsilon));
  }
}
