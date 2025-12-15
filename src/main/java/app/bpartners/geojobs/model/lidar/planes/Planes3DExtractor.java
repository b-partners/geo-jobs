package app.bpartners.geojobs.model.lidar.planes;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public class Planes3DExtractor implements Function<Collection<LasPointGeometry>, List<Plane3D>> {
  private final int minimumPointsCount;
  private final OnePlane3DExtractor onePlane3DExtractor;
  private final Plane3DContinuationCluster plane3DContinuationCluster;
  private final Plane3DMerger plane3DMerger;

  public Planes3DExtractor(
      int iteration,
      int minimumPointsCount,
      double pointThreshold,
      double pointContinuationThreshold,
      double maxPlane2DAreaToMerge,
      double planeDistanceEpsilonToMerge,
      double planeSlopeEpsilonToMerge,
      double delimitationConcaveRatio,
      double delimitationSimplificationEpsilon) {
    this.minimumPointsCount = minimumPointsCount;
    this.onePlane3DExtractor =
        new OnePlane3DExtractor(
            iteration, pointThreshold, delimitationConcaveRatio, delimitationSimplificationEpsilon);
    this.plane3DContinuationCluster =
        new Plane3DContinuationCluster(pointContinuationThreshold, minimumPointsCount);
    this.plane3DMerger =
        new Plane3DMerger(
            planeSlopeEpsilonToMerge, planeDistanceEpsilonToMerge, maxPlane2DAreaToMerge);
  }

  @Override
  public List<Plane3D> apply(Collection<LasPointGeometry> points) {
    List<Plane3D> planes = new ArrayList<>();
    List<LasPointGeometry> pointsToProcess = new ArrayList<>(points);

    while (pointsToProcess.size() > minimumPointsCount) {
      var result = onePlane3DExtractor.apply(pointsToProcess);
      var newPlane = result.plane();

      if (newPlane.getPoints().size() < minimumPointsCount) {
        break;
      }

      var clusterResult = plane3DContinuationCluster.apply(newPlane);
      var continuedPlane = clusterResult.plane();
      if (continuedPlane.getPoints().size() < minimumPointsCount) {
        break;
      }

      planes.add(continuedPlane);
      pointsToProcess = new ArrayList<>(result.outliers());
      pointsToProcess.addAll(clusterResult.outliers());
    }

    return plane3DMerger.apply(planes);
  }
}
