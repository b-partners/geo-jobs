package app.bpartners.geojobs.endpoint.rest.postprocessing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.function.Predicate;
import lombok.AllArgsConstructor;
import org.locationtech.jts.geom.*;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class GeoJsonValidator implements Predicate<File> {
  private final ObjectMapper mapper;
  private final GeometryFactory geometryFactory = new GeometryFactory();

  @Override
  public boolean test(File file) {
    try {
      String content = Files.readString(file.toPath());
      JsonNode root = mapper.readTree(content);
      JsonNode features = root.get("features");

      if (features == null || !features.isArray())
        throw new IllegalArgumentException("Missing or invalid 'features' array");

      for (int i = 0; i < features.size(); i++) {
        JsonNode feature = features.get(i);
        JsonNode geometryNode = feature.get("geometry");

        if (geometryNode == null || !geometryNode.get("type").asText().equals("Polygon"))
          throw new IllegalArgumentException("Feature #" + i + " has invalid geometry type");

        JsonNode coordinatesNode = geometryNode.get("coordinates");
        if (coordinatesNode == null || !coordinatesNode.isArray())
          throw new IllegalArgumentException(
              "Feature #" + i + " has missing or malformed coordinates");

        Polygon polygon = parsePolygon(coordinatesNode);
        if (polygon == null || !polygon.isValid())
          throw new IllegalArgumentException(
              "Feature #" + i + " contains an invalid polygon geometry");
      }
      return true;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private Polygon parsePolygon(JsonNode coordinatesArray) {
    var rings = new ArrayList<LinearRing>();
    for (JsonNode ringCoords : coordinatesArray) {
      LinearRing ring = createRing(ringCoords);
      if (ring == null) throw new IllegalArgumentException("Invalid ring structure");
      rings.add(ring);
    }

    LinearRing shell = rings.getFirst();
    LinearRing[] holes =
        rings.size() > 1 ? rings.subList(1, rings.size()).toArray(new LinearRing[0]) : null;

    return geometryFactory.createPolygon(shell, holes);
  }

  private LinearRing createRing(JsonNode coords) {
    var coordinateList = new ArrayList<Coordinate>();
    for (JsonNode point : coords) {
      if (!point.isArray() || point.size() < 2) return null;
      coordinateList.add(new Coordinate(point.get(0).asDouble(), point.get(1).asDouble()));
    }
    return geometryFactory.createLinearRing(coordinateList.toArray(new Coordinate[0]));
  }
}
