package app.bpartners.geojobs;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import com.github.mreutegg.laszip4j.LASReader;
import java.io.File;
import java.util.ArrayList;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

@Slf4j
public class LasFilePointsCount {
  private static final int BATCH_SIZE = 5_000;

  public static long count(File las, Polygon polygon) {
    var reader = new LASReader(las);
    var points = new ArrayList<LasPointGeometry>();
    var envelope = polygon.getEnvelopeInternal();
    var subReader =
        reader.insideRectangle(
            envelope.getMinX(), envelope.getMinY(), envelope.getMaxX(), envelope.getMaxY());
    var header = reader.getHeader();

    log.info("Reading {}", las.getName());
    long i = 1;
    for (var lasPoint : subReader.getPoints()) {
      if (i % BATCH_SIZE == 0) {
        log.info("i={}", i);
      }
      var point = new LasPointGeometry(lasPoint, header);
      if (polygon.contains(point)) {
        points.add(point);
        i++;
      }
    }

    log.info("Finished Reading {}", las.getName());

    log.info("Start Filter on a ArrayList");
    i = 0;
    for (var point : points) {
      if (polygon.contains(point)) {
        i++;
      }
    }
    log.info("Finished Filter on a ArrayList");
    return i;
  }

  private static Polygon roof12Polygon() {
    var coordinates =
        new Coordinate[] {
          new Coordinate(862022.2760225505, 6440730.539504434),
          new Coordinate(862105.0812116335, 6440727.50244971),
          new Coordinate(862104.5267584766, 6440708.781120122),
          new Coordinate(862098.9871669820, 6440707.943784346),
          new Coordinate(862098.6722462546, 6440666.710976321),
          new Coordinate(862075.7196985447, 6440667.151647371),
          new Coordinate(862077.4061275731, 6440709.113058433),
          new Coordinate(862049.9110695693, 6440710.474623096),
          new Coordinate(862047.8861269893, 6440668.157985783),
          new Coordinate(862026.2688625795, 6440670.712210407),
          new Coordinate(862027.6533974777, 6440710.933578276),
          new Coordinate(862022.0776989069, 6440711.481064914),
          new Coordinate(862022.2760225505, 6440730.539504434)
        };

    return geometryFactory.createPolygon(coordinates);
  }

  private static Polygon largeRoofPolygon() {
    var coordinates =
        new Coordinate[] {
          new Coordinate(701559.9855221047, 7029904.950539844),
          new Coordinate(701455.2756556942, 7029837.457126396),
          new Coordinate(701670.0264435381, 7029535.210221993),
          new Coordinate(701768.8830926412, 7029607.899278908),
          new Coordinate(701559.9855221047, 7029904.950539844)
        };
    return geometryFactory.createPolygon(coordinates);
  }

  public static void main(String[] args) {
    // var file = new File("/home/ricka/Works/Indexation/large/large_roof.copc.laz");
    var file = new File("/home/ricka/Works/Indexation/12/12_chimney.copc.laz");
    log.info("PointsCount={}", count(file, roof12Polygon()));
  }
}
