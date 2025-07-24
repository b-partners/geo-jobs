package app.bpartners.geojobs.service.lidar.model;

import static java.util.stream.Collectors.toSet;

import app.bpartners.geojobs.model.geometry.IndexedGeometries;
import com.github.mreutegg.laszip4j.LASHeader;
import com.github.mreutegg.laszip4j.LASReader;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Geometry;

@Slf4j
public class IndexedLas {
  private final IndexedGeometries indexedGeometries;
  private final LASHeader lasHeader;
  private final Set<LidarClass> classifications;

  public IndexedLas(File lasFile) {
    this(lasFile, new HashSet<>());
  }

  public IndexedLas(File lasFile, Set<LidarClass> classesToKeep) {
    var lasReader = new LASReader(lasFile);
    lasHeader = lasReader.getHeader();
    classifications = classesToKeep;

    log.info("Reading lasPoints from: " + lasFile.getPath());
    indexedGeometries = lasPoints(lasReader);
  }

  private IndexedGeometries lasPoints(LASReader lasReader) {
    Set<Geometry> jtsPoints = new HashSet<>();
    var nbPoints = 0;
    var isClassificationEmpty = classifications.isEmpty();

    for (var lasPoint : lasReader.getPoints()) {
      if (++nbPoints % 1_000_000 == 0) {
        log.info("Number of lasPoints read: " + nbPoints);
      }

      if (!isClassificationEmpty
          && !classifications.contains(LidarClass.fromValue(lasPoint.getClassification()))) {
        continue;
      }

      jtsPoints.add(new LasPointGeometry(lasPoint, lasHeader));
    }

    log.info("Total number of lasPoints read: " + nbPoints);
    return new IndexedGeometries(jtsPoints);
  }

  public Set<LasPointGeometry> containedIn(
      Geometry container, Predicate<LasPointGeometry> predicate) {
    return indexedGeometries
        .containedIn(container, (g) -> predicate.test((LasPointGeometry) predicate))
        .stream()
        .map(this::toLasPoint)
        .collect(toSet());
  }

  public Set<LasPointGeometry> containedIn(Geometry container) {
    return indexedGeometries.containedIn(container).stream().map(this::toLasPoint).collect(toSet());
  }

  private LasPointGeometry toLasPoint(Geometry geometry) {
    if (geometry instanceof LasPointGeometry) {
      return (LasPointGeometry) geometry;
    }

    throw new RuntimeException(
        "All geometries obtained from LAS file must be points, yet got: " + geometry);
  }
}
