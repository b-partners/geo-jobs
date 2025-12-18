package app.bpartners.geojobs.model.lidar.planes;

import static app.bpartners.geojobs.model.lidar.planes.exporter.Plane3DExtractionStep.*;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import app.bpartners.geojobs.model.lidar.planes.exporter.Plane3DExtractionStepExporter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class Planes3DExtractor implements Function<Collection<LasPointGeometry>, List<Plane3D>> {
  private final Plane3DExtractorConf conf;
  private final Plane3DMerger plane3DMerger;
  private final Plane3DExtractionStepExporter exporter;
  private final OnePlane3DExtractor onePlane3DExtractor;
  private final Plane3DContinuationCluster plane3DContinuationCluster;
  private static final int MIN_VALID_POLYGON_POINTS_COUNT = 3;

  public Planes3DExtractor(Plane3DExtractorConf conf) {
    this(conf, null);
  }

  public Planes3DExtractor(Plane3DExtractorConf conf, Plane3DExtractionStepExporter exporter) {
    this.conf = conf;
    this.exporter = exporter;
    this.onePlane3DExtractor =
        new OnePlane3DExtractor(
            conf.planeExtractionConf().iteration(),
            conf.planeExtractionConf().pointThreshold(),
            conf.planeDelimitationConf().concaveRatio(),
            conf.planeDelimitationConf().simplificationEpsilon(),
            exporter);
    this.plane3DContinuationCluster =
        new Plane3DContinuationCluster(
            conf.planeExtractionConf().pointContinuationThreshold(),
            conf.planeConf().minPointsCount());
    this.plane3DMerger =
        new Plane3DMerger(
            conf.planeMergerConf().max2DArea(),
            conf.planeMergerConf().slopeEpsilon(),
            conf.planeMergerConf().distanceEpsilon());
  }

  @Override
  public List<Plane3D> apply(Collection<LasPointGeometry> points) {
    List<Plane3D> planes = new ArrayList<>();
    List<LasPointGeometry> pointsToProcess = new ArrayList<>(points);

    var doExport = exporter != null;
    if (doExport) {
      exporter.export(INIT_POINTS, pointsToProcess);
    }

    int i = 0;
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

      if (doExport) {
        var subExporter = exporter.subSuffix(String.valueOf(++i));
        subExporter.export(RAW_PLANE_EXTRACTION, result.plane().getPoints());
        subExporter.export(PLANE_CONTINUITY_EXTRACTION, clusterResult.plane().getPoints());
      }
    }

    return mergeClosedPlaneAndFilterSmallOnes(planes);
  }

  public List<Plane3D> mergeClosedPlaneAndFilterSmallOnes(Collection<Plane3D> planes) {
    var mergedPlanes = filterInvalidPlanes(plane3DMerger.apply(planes));

    if (exporter == null) {
      return mergedPlanes;
    }

    var i = new AtomicInteger(0);
    return mergedPlanes.stream()
        .map(
            merged -> {
              var subExporter = exporter.subSuffix(String.valueOf(i.incrementAndGet()));
              subExporter.export(CLOSED_PLANE_MERGING, merged.getPoints());
              return merged.toBuilder().exporter(subExporter).build();
            })
        .toList();
  }

  private List<Plane3D> filterInvalidPlanes(Collection<Plane3D> planes) {
    return planes.stream()
        .filter(
            plane ->
                plane.getDelimitation().getCoordinates().length >= MIN_VALID_POLYGON_POINTS_COUNT
                    && plane.get2DArea() > conf.planeConf().min2DArea())
        .toList();
  }
}
