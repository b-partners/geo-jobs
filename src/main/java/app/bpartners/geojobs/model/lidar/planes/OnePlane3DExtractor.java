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
  private final Plane3DExtractorConf conf;
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

    var boxConf = conf.kernelConf();
    var kernelConf = conf.kernelConf();
    var planeExtractionConf = conf.planeExtractionConf();
    var planeDelimitationConf = conf.planeDelimitationConf();

    for (int i = 0; i < planeExtractionConf.iteration(); i++) {
      var kernel =
          Kernel.from(
              points,
              random,
              kernelConf.attempts(),
              kernelConf.threshold(),
              kernelConf.minVectorNorm(),
              kernelConf.orthogonalDegEpsilon());
      if (kernel.isEmpty()) continue;

      // --- 2. Fit plane
      var box =
          new Box(
              kernel.get(),
              boxConf.threshold(),
              planeDelimitationConf.concaveRatio(),
              planeDelimitationConf.simplificationEpsilon(),
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

  public record Result(Plane3D plane, List<LasPointGeometry> outliers) {}
}
