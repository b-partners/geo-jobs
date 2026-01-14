package app.bpartners.geojobs.model.lidar.planes;

import static app.bpartners.geojobs.model.lidar.planes.exporter.Plane3DExtractionStep.*;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import app.bpartners.geojobs.model.lidar.planes.exporter.Plane3DExtractionStepExporter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.locationtech.jts.math.Vector2D;

public class Planes3DExtractor implements Function<Collection<LasPointGeometry>, List<Plane3D>> {
  private final Plane3DExtractorConf conf;
  // TODO: Improve optional Plane3DMerger
  private final Plane3DMerger plane3DMerger;
  private final Plane3DExtractionStepExporter exporter;
  private final OnePlane3DExtractor onePlane3DExtractor;
  private static final int MIN_VALID_POLYGON_POINTS_COUNT = 4;

  public Planes3DExtractor(Plane3DExtractorConf conf) {
    this(conf, null);
  }

  public Planes3DExtractor(Plane3DExtractorConf conf, Plane3DExtractionStepExporter exporter) {
    this.conf = conf;
    this.exporter = exporter;

    this.plane3DMerger =
        new Plane3DMerger(
            conf.planeMergerConf().slopeEpsilon(), conf.planeMergerConf().distanceEpsilon());

    var continuationCluster =
        new LasPointContinuationCluster(
            conf.planeExtractionConf().pointContinuationThreshold(),
            conf.planeConf().minPointsCount());

    this.onePlane3DExtractor = new OnePlane3DExtractor(conf, exporter, continuationCluster);
  }

  @Override
  public List<Plane3D> apply(Collection<LasPointGeometry> points) {
    List<Plane3D> planes = new ArrayList<>();
    Set<LasPointGeometry> toUsedAsKernel = new HashSet<>(points);
    Set<LasPointGeometry> pointsToProcess = new HashSet<>(points);
    Set<LasPointGeometry> delimitations = new HashSet<>();

    var doExport = exporter != null;
    if (doExport) {
      exporter.export(INIT_POINTS, points);
    }

    int i = 0;
    var minPointsCount = conf.planeConf().minPointsCount();
    while (toUsedAsKernel.size() > minPointsCount) {
      var result = onePlane3DExtractor.apply(pointsToProcess, toUsedAsKernel);
      var newPlane = result.plane();

      if (newPlane.getPoints().size() < minPointsCount) {
        break;
      }

      planes.add(newPlane);
      toUsedAsKernel = result.outliers();

      pointsToProcess = new HashSet<>(result.outliers());
      delimitations.addAll(newPlane.getDelimitationPoints());
      pointsToProcess.addAll(delimitations);

      if (doExport) {
        var subExporter = exporter.subSuffix(String.valueOf(++i));
        subExporter.export(RAW_PLANE_EXTRACTION, result.plane().getPoints());
        subExporter.export(ITERATION_POINTS_EVOLUTION, pointsToProcess);
      }
    }

    return mergeClosedPlaneAndFilterSmallOnes(planes);
  }

  public List<Plane3D> mergeClosedPlaneAndFilterSmallOnes(Collection<Plane3D> planes) {
    // var mergedPlanes = filterInvalidPlanes(plane3DMerger.apply(planes));
    var mergedPlanes = filterInvalidPlanes(planes);

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
            plane -> {
              var coordinates = plane.getDelimitation().getCoordinates();

              if (coordinates.length < MIN_VALID_POLYGON_POINTS_COUNT) {
                return false;
              }

              if (plane.get2DArea() <= conf.planeConf().min2DArea()) {
                return false;
              }

              var directions = toDirections(plane);
              return hasTwoNonParallelDirections(directions);
            })
        .toList();
  }

  private List<Vector2D> toDirections(Plane3D plane) {
    List<Vector2D> directions = new ArrayList<>();
    var coordinates = plane.getDelimitation().getCoordinates();

    for (int i = 0; i < coordinates.length - 1; i++) {
      var a = coordinates[i];
      var b = coordinates[i + 1];

      var edge = Vector2D.create(a, b);
      double len = edge.length();

      if (len > conf.planeConf().minEdgeLength()) {
        directions.add(edge.normalize());
      }
    }
    return directions;
  }

  private boolean hasTwoNonParallelDirections(List<Vector2D> directions) {
    int n = directions.size();
    if (n < 2) {
      return false;
    }

    for (int i = 0; i < n; i++) {
      for (int j = i + 1; j < n; j++) {
        double dot = Math.abs(directions.get(i).dot(directions.get(j)));

        if (dot < 1.0 - getEpsilonParallel(conf.planeConf().parallelDirectionEpsilon())) {
          return true;
        }
      }
    }
    return false;
  }

  private static double getEpsilonParallel(double angle) {
    return 1.0 - Math.cos(Math.toRadians(angle));
  }
}
