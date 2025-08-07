package app.bpartners.geojobs.service.lidar;

import static app.bpartners.geojobs.service.GeometrySquareMeterArea.LAMBERT_93;
import static app.bpartners.geojobs.service.GeometrySquareMeterArea.WGS84;
import static app.bpartners.geojobs.service.lidar.model.LidarClass.*;

import app.bpartners.geojobs.service.GeometrySquareMeterArea;
import app.bpartners.geojobs.service.lidar.model.*;
import java.io.File;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LidarPolygonMetricProcessor implements Function<List<Polygon>, List<Dimension>> {
  private final LidarApi lidarApi;
  private final GeometrySquareMeterArea projector;
  private static final int SOL_BUFFER_METERS = 3;

  @Override
  public List<Dimension> apply(List<Polygon> roofGeometries) {
    var projectedRoofGeometries =
        roofGeometries.stream().map(g -> projector.project(g, WGS84, LAMBERT_93)).toList();
    var downloadedLidarFiles = lidarApi.apply(projectedRoofGeometries);
    var solGeometries =
        projectedRoofGeometries.stream().map(g -> g.buffer(SOL_BUFFER_METERS)).toList();

    return getDimensionsFromMultipleFiles(
        projectedRoofGeometries, solGeometries, downloadedLidarFiles);
  }

  private List<Dimension> getDimensionsFromMultipleFiles(
      List<Geometry> roofGeometries, List<Geometry> solGeometries, Set<File> lidarFiles) {
    List<Dimension> results = roofGeometries.stream().map(g -> Dimension.empty()).toList();

    for (var lidarFile : lidarFiles) {
      var dimensions = getDimensions(roofGeometries, solGeometries, lidarFile);

      for (int i = 0; i < results.size(); i++) {
        results.get(i).roof().addAll(dimensions.get(i).roof().points());
        results.get(i).sol().addAll(dimensions.get(i).sol().points());
      }
    }

    return results;
  }

  private List<Dimension> getDimensions(
      List<Geometry> roofGeometries, List<Geometry> solGeometries, File file) {
    List<Dimension> dimensions = new ArrayList<>();
    var indexedLas = new IndexedLas(file, Set.of(BATIMENT, SOL));

    for (int i = 0; i < roofGeometries.size(); i++) {
      var roofGeometry = roofGeometries.get(i);
      var solGeometry = solGeometries.get(i);

      var roofPoints = indexedLas.containedIn(roofGeometry, testClassification(BATIMENT));
      log.info("[{}] Found {} roof points in file: {}", i, roofPoints.size(), file.getName());

      var solPoints = indexedLas.containedIn(solGeometry, testClassification(SOL));
      log.info("[{}] Found {} sol points in file: {}", i, solPoints.size(), file.getName());

      var roof = new Roof(roofPoints);
      var sol = new Sol(solPoints);
      dimensions.add(new Dimension(roof, sol));
    }

    return dimensions;
  }

  private static Predicate<LasPointGeometry> testClassification(LidarClass expectedClass) {
    return p -> expectedClass.equals(p.getClassification());
  }
}
