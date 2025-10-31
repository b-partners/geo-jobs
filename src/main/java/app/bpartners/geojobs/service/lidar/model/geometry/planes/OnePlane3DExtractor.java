package app.bpartners.geojobs.service.lidar.model.geometry.planes;

import static java.util.stream.Collectors.toList;

import app.bpartners.geojobs.service.lidar.model.geometry.LasPointGeometry;
import java.util.*;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OnePlane3DExtractor
    implements Function<List<LasPointGeometry>, OnePlane3DExtractor.Result> {
  private final int iterations;
  private final double threshold;
  private static final Random RANDOM = new Random();

  @Override
  public Result apply(List<LasPointGeometry> points) {
    if (points.size() < 3) {
      throw new IllegalArgumentException("At least 3 points are required.");
    }

    List<LasPointGeometry> bestInliers = new ArrayList<>();
    Plane3D bestModel = null;

    for (int i = 0; i < iterations; i++) {
      // --- 1. Sample 3 distinct random points
      var p1 = points.get(RANDOM.nextInt(points.size()));
      var p2 = points.get(RANDOM.nextInt(points.size()));
      var p3 = points.get(RANDOM.nextInt(points.size()));

      // Ensure distinct points
      if (p1.equals(p2) || p2.equals(p3) || p1.equals(p3)) {
        continue;
      }

      // --- 2. Fit plane
      var plane = Plane3D.fit(p1, p2, p3);

      // --- 3. Compute distances
      var inliers = points.stream().filter(p -> plane.distance(p) < threshold).collect(toList());

      // --- 4. Keep best
      if (inliers.size() > bestInliers.size()) {
        bestInliers = inliers;
        bestModel = plane;
      }
    }

    if (bestInliers.isEmpty()) {
      return new Result(new Plane3D(0, 0, 0, 0, new HashSet<>()), points);
    }

    // --- 5. Compute outliers
    var inlierSet = new HashSet<>(bestInliers);
    var outliers = points.stream().filter(p -> !inlierSet.contains(p)).collect(toList());
    return new Result(bestModel.with(inlierSet), outliers);
  }

  public record Result(Plane3D plane, List<LasPointGeometry> outliers) {}
}
