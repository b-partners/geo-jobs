package app.bpartners.geojobs.model.lidar.planes;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import app.bpartners.geojobs.model.lidar.planes.exporter.Plane3DExtractionStepExporter;
import java.security.SecureRandom;
import java.util.*;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OnePlane3DExtractor
    implements Function<List<LasPointGeometry>, OnePlane3DExtractor.Result> {
  private final int iterations;
  private final double threshold;
  private final double delimitationConcaveRatio;
  private final double delimitationSimplificationEpsilon;
  private final Plane3DExtractionStepExporter exporter;
  private final Plane3DContinuationCluster continuationCluster;

  @Override
  public Result apply(List<LasPointGeometry> points) {
    if (points.size() < 3) {
      throw new IllegalArgumentException("At least 3 points are required.");
    }

    List<LasPointGeometry> bestInliers = new ArrayList<>();
    Plane3D bestModel = null;

    var random = new SecureRandom();
    for (int i = 0; i < iterations; i++) {
      // --- 1. Sample 3 distinct random points
      var tripletOpt = sampleOrthogonalTriplet(points, random, threshold * 3, 0.05, 20);

      if (tripletOpt.isEmpty()) {
        continue;
      }

      var triplet = tripletOpt.get();
      var p1 = triplet[0];
      var p2 = triplet[1];
      var p3 = triplet[2];

      // Ensure distinct points
      if (p1 == p2 || p2 == p3 || p1 == p3) {
        continue;
      }

      // --- 2. Fit plane
      var box =
          new Box(
              p1,
              p2,
              p3,
              threshold,
              delimitationConcaveRatio,
              delimitationSimplificationEpsilon,
              exporter);

      // --- 3. Compute distances
      box.add(points);
      var inliers = box.getPoints();

      // --- 4. Keep best
      if (inliers.size() < bestInliers.size()) {
        continue;
      }

      var afterCluster = continuationCluster.apply(box.getPlane().with(inliers));
      var clusterPlane = afterCluster.plane();
      if (clusterPlane.getPoints().size() > bestInliers.size()) {
        bestModel = clusterPlane;
        bestInliers = clusterPlane.getPoints();
      }
    }

    if (bestInliers.isEmpty()) {
      return new Result(Plane3D.empty(), points);
    }

    // --- 5. Compute outliers
    var inlierSet = new HashSet<>(bestInliers);
    var outliers = points.stream().filter(p -> !inlierSet.contains(p)).toList();
    return new Result(bestModel.with(bestInliers), outliers);
  }

  private Optional<LasPointGeometry[]> sampleOrthogonalTriplet(
      List<LasPointGeometry> points,
      SecureRandom random,
      double neighborRadius,
      double angleEpsilon,
      int maxTries) {
    var p1 = points.get(random.nextInt(points.size()));

    var neighbors =
        points.stream().filter(p -> p != p1 && p1.distance(p) < neighborRadius).toList();

    if (neighbors.size() < 2) {
      return Optional.empty();
    }

    for (int i = 0; i < maxTries; i++) {
      var p2 = neighbors.get(random.nextInt(neighbors.size()));
      var p3 = neighbors.get(random.nextInt(neighbors.size()));

      if (p2 == p3) continue;

      var v1 = Vec3.from(p1, p2);
      var v2 = Vec3.from(p1, p3);

      var n1 = v1.norm();
      var n2 = v2.norm();
      if (n1 < 1e-6 || n2 < 1e-6) continue;

      double cos = Math.abs(v1.dot(v2) / (n1 * n2));

      if (cos < angleEpsilon) {
        return Optional.of(new LasPointGeometry[] {p1, p2, p3});
      }
    }

    return Optional.empty();
  }

  record Vec3(double x, double y, double z) {

    static Vec3 from(LasPointGeometry a, LasPointGeometry b) {
      return new Vec3(b.getX() - a.getX(), b.getY() - a.getY(), b.getZ() - a.getZ());
    }

    double dot(Vec3 o) {
      return x * o.x + y * o.y + z * o.z;
    }

    double norm() {
      return Math.sqrt(dot(this));
    }
  }

  public record Result(Plane3D plane, List<LasPointGeometry> outliers) {}
}
