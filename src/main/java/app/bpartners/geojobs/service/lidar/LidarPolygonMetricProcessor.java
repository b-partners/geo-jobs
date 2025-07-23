package app.bpartners.geojobs.service.lidar;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.service.GeometrySquareMeterArea.LAMBERT_93;
import static app.bpartners.geojobs.service.GeometrySquareMeterArea.WGS84;
import static java.lang.Float.compare;
import static java.lang.Runtime.getRuntime;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.stream.Collectors.toSet;

import app.bpartners.geojobs.service.GeometrySquareMeterArea;
import com.github.mreutegg.laszip4j.LASPoint;
import com.github.mreutegg.laszip4j.LASReader;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class LidarPolygonMetricProcessor
    implements BiFunction<Polygon, Set<File>, Dimension> {
  private static final int LIDAR_SOL_CLASSE = 2;
  private static final int LIDAR_BATIMENT_CLASSE = 6;
  private final GeometrySquareMeterArea projector;


  @Override
  public Dimension apply(Polygon roofGeometry, Set<File> lidarFiles) {
    var projectedRoof = projector.project(roofGeometry, WGS84, LAMBERT_93);
    var file = new ArrayList<>(lidarFiles).getFirst();
    return getLidarPoints(projectedRoof, file);
  }


  private Dimension getLidarPoints(Geometry polygon, File file) {
    var roofEnvelope = polygon.getEnvelopeInternal();
    var lasReader = new LASReader(file);
    var roofPoints = new HashSet<LidarPoint>();
    var solPoints = new HashSet<LidarPoint>();
    var lasReaderPoints = lasReader.getPoints();
    var lasHeader = lasReader.getHeader();
    var xScale = lasHeader.getXScaleFactor();
    var xOffset = lasHeader.getXOffset();
    var yScale = lasHeader.getYScaleFactor();
    var yOffset = lasHeader.getYOffset();

    var filtered = lasReader.insideRectangle(
            roofEnvelope.getMinX(), roofEnvelope.getMinY(), roofEnvelope.getMaxX(), roofEnvelope.getMaxY()).getPoints();

    for (var p : filtered) {
      int label = p.getClassification();

      if (label == LIDAR_BATIMENT_CLASSE) {
        roofPoints.add(new LidarPoint(p, label));
        continue;
      }
      if (label == LIDAR_SOL_CLASSE){
        solPoints.add(new LidarPoint(p, label));
      }
    }
    var roof = new Roof(roofPoints);
    var sol = new Sol(solPoints);
    return new Dimension(roof, sol);
  }
}
