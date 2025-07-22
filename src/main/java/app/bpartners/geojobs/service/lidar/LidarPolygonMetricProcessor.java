package app.bpartners.geojobs.service.lidar;

import static app.bpartners.geojobs.model.geometry.polygon.PolygonReprojection.EPSG_2154;
import static app.bpartners.geojobs.model.geometry.polygon.PolygonReprojection.EPSG_4326;
import static java.util.concurrent.Executors.newFixedThreadPool;

import app.bpartners.geojobs.model.geometry.GeometryFactory;
import app.bpartners.geojobs.model.geometry.polygon.PolygonReprojection;
import com.github.mreutegg.laszip4j.LASPoint;
import com.github.mreutegg.laszip4j.LASReader;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.BiFunction;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

public class LidarPolygonMetricProcessor
    implements BiFunction<Polygon, Set<File>, LidarPolygonMetricProcessor.LidarPolygonMetric> {
  private static final int BUFFER_SOL_METERS = 2;
  private static final int LIDAR_SOL_CLASSE = 2;
  private static final int LIDAR_BATIMENT_CLASSE = 6;
  private final PolygonReprojection polygonReprojection;

  public LidarPolygonMetricProcessor() {
    this.polygonReprojection = new PolygonReprojection(EPSG_4326, EPSG_2154);
  }

  @Override
  public LidarPolygonMetric apply(Polygon polygon, Set<File> lidarFiles) {
    var reprojectedPolygon = this.polygonReprojection.apply(polygon);
    var allPoints = readLidarPointsInParallel(reprojectedPolygon, lidarFiles);
    var polygonPoints = allPoints.polygonPoints();
    var solPoints = allPoints.solPoints();

    if (polygonPoints.size() < 2 || solPoints.size() < 2) {
      return new LidarPolygonMetric(polygon, 0, 0);
    }

    var minZPoint =
        polygonPoints.stream().min((a, b) -> Float.compare(a.getZ(), b.getZ())).orElseThrow();
    var maxZPoint =
        polygonPoints.stream().max((a, b) -> Float.compare(a.getZ(), b.getZ())).orElseThrow();

    var slopeInDegree = calculateSlopeInDegrees(minZPoint, maxZPoint);
    var heightInMeter = calculateHeightInMeters(solPoints, minZPoint);
    return new LidarPolygonMetric(polygon, slopeInDegree, heightInMeter);
  }

  private PolygonPointsAndSolPoints readLidarPointsInParallel(
      Polygon reprojectedPolygon, Set<File> lidarFiles) {
    var polygonPoints = new ArrayList<LASPoint>();
    var solPoints = new ArrayList<LASPoint>();

    var executor =
        newFixedThreadPool(Math.min(lidarFiles.size(), Runtime.getRuntime().availableProcessors()));
    List<Future<PolygonPointsAndSolPoints>> futures = new ArrayList<>();

    for (var lidarFile : lidarFiles) {
      futures.add(
          executor.submit(
              () -> {
                var lasReader = new LASReader(lidarFile);
                return getPolygonPointsAndSolPoints(reprojectedPolygon, lasReader);
              }));
    }

    try {
      for (Future<PolygonPointsAndSolPoints> future : futures) {
        var result = future.get();
        polygonPoints.addAll(result.polygonPoints());
        solPoints.addAll(result.solPoints());
      }
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException("Error while reading a LIDAR file", e);
    } finally {
      executor.shutdown();
    }

    return new PolygonPointsAndSolPoints(polygonPoints, solPoints);
  }

  private static double calculateSlopeInDegrees(LASPoint minZPoint, LASPoint maxZPoint) {
    double dx = maxZPoint.getX() - minZPoint.getX();
    double dy = maxZPoint.getY() - minZPoint.getY();
    double dz = maxZPoint.getZ() - minZPoint.getZ();
    double distance = Math.sqrt(dx * dx + dy * dy);

    if (distance > 0) {
      return round2(Math.toDegrees(Math.atan(dz / distance)));
    }

    return 0;
  }

  private static double calculateHeightInMeters(List<LASPoint> solPoints, LASPoint minZPoint) {
    double meanSolZ = solPoints.stream().mapToDouble(LASPoint::getZ).average().orElseThrow();

    return round2(minZPoint.getZ() - meanSolZ);
  }

  private static PolygonPointsAndSolPoints getPolygonPointsAndSolPoints(
      Polygon polygon, LASReader lasReader) {
    var solGeometry = polygon.buffer(BUFFER_SOL_METERS);
    var geometryFactory = GeometryFactory.geometryFactory;
    var solPoints = new ArrayList<LASPoint>();
    var polygonPoints = new ArrayList<LASPoint>();

    for (var point : lasReader.getPoints()) {
      int pointClassification = point.getClassification();
      var geometryPoint = geometryFactory.createPoint(new Coordinate(point.getX(), point.getY()));

      if (pointClassification == LIDAR_BATIMENT_CLASSE) {
        if (polygon.contains(geometryPoint)) {
          polygonPoints.add(point);
        }
        continue;
      }

      if (pointClassification == LIDAR_SOL_CLASSE) {
        if (solGeometry.contains(geometryPoint)) {
          solPoints.add(point);
        }
      }
    }

    return new PolygonPointsAndSolPoints(polygonPoints, solPoints);
  }

  private static double round2(double value) {
    return Math.ceil(value * 100) / 100.0;
  }

  private record PolygonPointsAndSolPoints(
      List<LASPoint> polygonPoints, List<LASPoint> solPoints) {}

  public record LidarPolygonMetric(Polygon polygon, double slopeInDegrees, double heightInMeters) {}
}
