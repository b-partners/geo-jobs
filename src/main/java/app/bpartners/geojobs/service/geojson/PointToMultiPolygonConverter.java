package app.bpartners.geojobs.service.geojson;

import app.bpartners.gen.annotator.endpoint.rest.model.Point;
import java.io.StringWriter;
import java.util.function.BiFunction;
import lombok.SneakyThrows;
import org.geotools.geojson.geom.GeometryJSON;
import org.locationtech.jts.geom.*;
import org.springframework.stereotype.Component;

// Most ChatGPT-generated code
@Component
public class PointToMultiPolygonConverter implements BiFunction<Point, Double, MultiPolygon> {
  private static final double APPROXIMATE_METERS_PER_DEGREE_OF_LATITUDE = 111320.0;
  private final GeometryFactory geometryFactory = new GeometryFactory();

  @Override
  public MultiPolygon apply(Point point, Double sizeInMeters) {
    var latitude = point.getX();
    var longitude = point.getY();

    // 1. Convert meters to degrees
    double halfSize = sizeInMeters / 2.0;
    double deltaLat = halfSize / APPROXIMATE_METERS_PER_DEGREE_OF_LATITUDE;
    double deltaLon =
        halfSize / (APPROXIMATE_METERS_PER_DEGREE_OF_LATITUDE * Math.cos(Math.toRadians(latitude)));

    // 2. Define square corners
    Coordinate[] coordinates =
        new Coordinate[] {
          new Coordinate(longitude - deltaLon, latitude - deltaLat),
          new Coordinate(longitude + deltaLon, latitude - deltaLat),
          new Coordinate(longitude + deltaLon, latitude + deltaLat),
          new Coordinate(longitude - deltaLon, latitude + deltaLat),
          new Coordinate(longitude - deltaLon, latitude - deltaLat) // Close ring
        };

    // 3. Build polygon and wrap in MultiPolygon
    LinearRing shell = geometryFactory.createLinearRing(coordinates);
    Polygon polygon = geometryFactory.createPolygon(shell, null);
    return geometryFactory.createMultiPolygon(new Polygon[] {polygon});
  }

  @SneakyThrows
  public String generateSquareMultiPolygon(MultiPolygon multiPolygon) {
    GeometryJSON geometryJSON = new GeometryJSON();
    StringWriter writer = new StringWriter();
    geometryJSON.write(multiPolygon, writer);
    return writer.toString();
  }
}
