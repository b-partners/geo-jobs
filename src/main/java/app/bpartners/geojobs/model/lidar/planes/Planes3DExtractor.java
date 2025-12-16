package app.bpartners.geojobs.model.lidar.planes;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public class Planes3DExtractor implements Function<Collection<LasPointGeometry>, List<Plane3D>> {
  private final Plane3DExtractorConf conf;
  private final OnePlane3DExtractor onePlane3DExtractor;
  private final Plane3DContinuationCluster plane3DContinuationCluster;
  private final Plane3DMerger plane3DMerger;

  public Planes3DExtractor(Plane3DExtractorConf conf) {
    this.conf = conf;
    this.onePlane3DExtractor =
        new OnePlane3DExtractor(
            conf.planeExtractionConf().iteration(),
            conf.planeExtractionConf().pointThreshold(),
            conf.planeDelimitationConf().concaveRatio(),
            conf.planeDelimitationConf().simplificationEpsilon());
    this.plane3DContinuationCluster =
        new Plane3DContinuationCluster(
            conf.planeExtractionConf().pointContinuationThreshold(),
            conf.planeConf().minPointsCount());
    this.plane3DMerger =
        new Plane3DMerger(
            conf.planeMergerConf().slopeEpsilon(),
            conf.planeMergerConf().distanceEpsilon(),
            conf.planeMergerConf().max2DArea());
  }

  @Override
  public List<Plane3D> apply(Collection<LasPointGeometry> points) {
    List<Plane3D> planes = new ArrayList<>();
    List<LasPointGeometry> pointsToProcess = new ArrayList<>(points);

    var minPointsCount = conf.planeConf().minPointsCount();
    while (pointsToProcess.size() > minPointsCount) {
      var result = onePlane3DExtractor.apply(pointsToProcess);
      var newPlane = result.plane();

      if (newPlane.getPoints().size() < minPointsCount) {
        break;
      }

      var clusterResult = plane3DContinuationCluster.apply(newPlane);
      var continuedPlane = clusterResult.plane();
      if (continuedPlane.getPoints().size() < minPointsCount) {
        break;
      }

      planes.add(continuedPlane);
      pointsToProcess = new ArrayList<>(result.outliers());
      pointsToProcess.addAll(clusterResult.outliers());
    }

    return plane3DMerger.apply(planes);
  }
}
