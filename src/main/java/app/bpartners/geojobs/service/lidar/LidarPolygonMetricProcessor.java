package app.bpartners.geojobs.service.lidar;

import static app.bpartners.geojobs.service.GeometrySquareMeterArea.LAMBERT_93;
import static app.bpartners.geojobs.service.GeometrySquareMeterArea.WGS84;
import static app.bpartners.geojobs.service.lidar.model.LidarClass.*;
import static java.util.concurrent.Executors.newFixedThreadPool;

import app.bpartners.geojobs.service.GeometrySquareMeterArea;
import app.bpartners.geojobs.service.lidar.model.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class LidarPolygonMetricProcessor implements BiFunction<Polygon, Set<File>, Dimension> {
  private final GeometrySquareMeterArea projector;
  private static final int SOL_BUFFER_METERS = 2;

  @Override
  public Dimension apply(Polygon roofGeometry, Set<File> lidarFiles) {
    var projectedRoof = projector.project(roofGeometry, WGS84, LAMBERT_93);
    var solGeometry = roofGeometry.buffer(SOL_BUFFER_METERS);
    return getDimensionInParallel(projectedRoof, solGeometry, lidarFiles);
  }

  private Dimension getDimensionInParallel(
      Geometry projectedRoof, Geometry solGeometry, Set<File> lidarFiles) {
    Set<LasPointGeometry> roofPoints = new HashSet<>();
    Set<LasPointGeometry> solPoints = new HashSet<>();

    var executor =
        newFixedThreadPool(Math.min(lidarFiles.size(), Runtime.getRuntime().availableProcessors()));
    List<Future<Dimension>> futures = new ArrayList<>();

    for (var lidarFile : lidarFiles) {
      futures.add(executor.submit(() -> getDimension(projectedRoof, solGeometry, lidarFile)));
    }

    try {
      for (Future<Dimension> future : futures) {
        var result = future.get();
        roofPoints.addAll(result.roof().points());
        solPoints.addAll(result.sol().points());
      }
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException("Error while reading a LIDAR file", e);
    } finally {
      executor.shutdown();
    }

    var roof = new Roof(roofPoints);
    var sol = new Sol(solPoints);
    return new Dimension(roof, sol);
  }

  private Dimension getDimension(Geometry roofGeometry, Geometry solGeometry, File file) {
    var indexedLas = new IndexedLas(file, Set.of(SOL, BATIMENT));

    var roofPoints = indexedLas.containedIn(roofGeometry, testClassification(BATIMENT));
    var solPoints = indexedLas.containedIn(solGeometry, testClassification(SOL));

    var roof = new Roof(roofPoints);
    var sol = new Sol(solPoints);
    return new Dimension(roof, sol);
  }

  private static Predicate<LasPointGeometry> testClassification(LidarClass expectedClass) {
    return (g) -> {
      var candidateClass = fromValue(g.getLasPoint().getClassification());
      return expectedClass.equals(candidateClass);
    };
  }
}
