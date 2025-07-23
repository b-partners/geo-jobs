package app.bpartners.geojobs.model.las;

import static java.util.stream.Collectors.toSet;

import app.bpartners.geojobs.model.geometry.IndexedGeometries;
import com.github.mreutegg.laszip4j.LASReader;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

@Slf4j
public class IndexedLas {

  private final IndexedGeometries indexedGeometries;

  public IndexedLas(File lasFile) {
    indexedGeometries = lasPoints(lasFile);
  }

  private IndexedGeometries lasPoints(File lasFile) {
    Set<Geometry> jtsPoints = new HashSet<>();

    var lasReader = new LASReader(lasFile);
    var geometryFactory = new GeometryFactory();
    var nbPoints = 0;
    log.info("Reading lasPoints from: " + lasFile.getPath());
    for (var lasPoint : lasReader.getPoints()) {
      if (++nbPoints % 1_000_000 == 0) {
        log.info("Number of lasPoints read: " + nbPoints);
      }
      int x = lasPoint.getX();
      int y = lasPoint.getY();
      jtsPoints.add(geometryFactory.createPoint(new Coordinate(x, y)));
    }
    log.info("Total number of lasPoints read: " + nbPoints);

    return new IndexedGeometries(jtsPoints);
  }

  public Set<Point> containedIn(Geometry container) {
    return indexedGeometries.containedIn(container).stream().map(this::toPoint).collect(toSet());
  }

  private Point toPoint(Geometry geometry) {
    if (geometry instanceof Point) {
      return (Point) geometry;
    }
    throw new RuntimeException(
        "All geometries obtained from LAS file must be points, yet got: " + geometry);
  }
}
