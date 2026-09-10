package app.bpartners.geojobs.model.lidar.zone;

import static app.bpartners.geojobs.service.GeometrySquareMeterArea.EPSG_2056;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.util.GeometryFixer;
import org.springframework.core.io.ClassPathResource;

public final class SwissZone implements LidarZone {
  public static final SwissZone INSTANCE = new SwissZone();

  private final Geometry boundary;

  private SwissZone() {
    this.boundary = loadBoundary();
  }

  @Override
  public boolean contains(Geometry wgs84Geometry) {
    var fixed = GeometryFixer.fix(wgs84Geometry);
    return boundary.getEnvelopeInternal().intersects(fixed.getEnvelopeInternal())
        && boundary.contains(fixed);
  }

  @Override
  public CoordinateReferenceSystem getLocalCrs() {
    return EPSG_2056;
  }

  private static Geometry loadBoundary() {
    try {
      var resource = new ClassPathResource("files/swiss_boundary.geojson");
      var geoJson = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      return parseGeoJsonPolygon(geoJson);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static Geometry parseGeoJsonPolygon(String geoJson) throws Exception {
    var mapper = new ObjectMapper();
    var root = mapper.readTree(geoJson);
    var coordinates = "coordinates";
    var type = root.get("type").asText();
    var factory = new GeometryFactory(new PrecisionModel(), 4326);

    return switch (type) {
      case "Polygon" -> parsePolygon(root.get(coordinates), factory);
      case "MultiPolygon" -> {
        var polygonsNode = root.get(coordinates);
        var polygons = new Polygon[polygonsNode.size()];
        for (int i = 0; i < polygonsNode.size(); i++) {
          polygons[i] = parsePolygon(polygonsNode.get(i), factory);
        }
        yield factory.createMultiPolygon(polygons);
      }
      case "FeatureCollection" -> {
        var features = root.get("features");
        var polygons = new Polygon[features.size()];
        for (int i = 0; i < features.size(); i++) {
          var geom = features.get(i).get("geometry");
          polygons[i] = parsePolygon(geom.get(coordinates), factory);
        }
        yield factory.createMultiPolygon(polygons);
      }
      default -> throw new IllegalArgumentException("Unsupported GeoJSON type: " + type);
    };
  }

  private static Polygon parsePolygon(JsonNode coordinatesNode, GeometryFactory factory) {
    var outerRing = coordinatesNode.get(0);
    var coords = new Coordinate[outerRing.size()];
    for (int i = 0; i < outerRing.size(); i++) {
      var point = outerRing.get(i);
      coords[i] = new Coordinate(point.get(0).asDouble(), point.get(1).asDouble());
    }
    return factory.createPolygon(coords);
  }
}
