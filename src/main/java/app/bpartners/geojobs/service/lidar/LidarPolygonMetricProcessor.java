package app.bpartners.geojobs.service.lidar;

import static app.bpartners.geojobs.service.GeometrySquareMeterArea.LAMBERT_93;
import static app.bpartners.geojobs.service.GeometrySquareMeterArea.WGS84;
import static app.bpartners.geojobs.service.lidar.model.LidarClass.*;

import app.bpartners.geojobs.service.GeometrySquareMeterArea;
import app.bpartners.geojobs.service.lidar.model.*;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
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
    var projectedRoofGeometry = projector.project(roofGeometry, WGS84, LAMBERT_93);
    var solGeometry = projectedRoofGeometry.buffer(SOL_BUFFER_METERS);

    return getDimensionFromMultipleFiles(projectedRoofGeometry, solGeometry, lidarFiles);
  }

  private Dimension getDimensionFromMultipleFiles(
      Geometry roofGeometry, Geometry solGeometry, Set<File> lidarFiles) {
    Set<LasPointGeometry> roofPoints = new HashSet<>();
    Set<LasPointGeometry> solPoints = new HashSet<>();

    for (var lidarFile : lidarFiles) {
      var result = getDimension(roofGeometry, solGeometry, lidarFile);
      roofPoints.addAll(result.roof().points());
      solPoints.addAll(result.sol().points());
    }

    var roof = new Roof(roofPoints);
    var sol = new Sol(solPoints);

    return new Dimension(roof, sol);
  }

  private Dimension getDimension(Geometry roofGeometry, Geometry solGeometry, File file) {
    var indexedLas = new IndexedLas(file, Set.of(BATIMENT, SOL));

    var solPoints = indexedLas.containedIn(solGeometry, testClassification(SOL));
    var roofPoints = indexedLas.containedIn(roofGeometry, testClassification(BATIMENT));

    var roof = new Roof(roofPoints);
    var sol = new Sol(solPoints);
    return new Dimension(roof, sol);
  }

  private static Predicate<LasPointGeometry> testClassification(LidarClass expectedClass) {
    return (p) -> expectedClass.equals(p.getClassification());
  }
}
