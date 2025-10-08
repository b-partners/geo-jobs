package app.bpartners.geojobs.test;

import app.bpartners.geojobs.service.lidar.model.LidarClass;
import app.bpartners.geojobs.service.lidar.model.geometry.LasPointGeometry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import lombok.SneakyThrows;

public class GeoJsonLoader {
  @SneakyThrows
  public static Set<LasPointGeometry> readPoints(File geojsonFile) {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode root = mapper.readTree(geojsonFile);
    Set<LasPointGeometry> points = new HashSet<>();

    if (!root.has("features")) {
      throw new IllegalArgumentException("GeoJSON is missing 'features'");
    }

    for (JsonNode feature : root.get("features")) {
      JsonNode geometry = feature.get("geometry");
      if (geometry == null || !"Point".equals(geometry.get("type").asText())) {
        continue;
      }
      JsonNode coords = geometry.get("coordinates");
      if (coords == null || coords.size() < 2) {
        continue;
      }

      double x = coords.get(0).asDouble();
      double y = coords.get(1).asDouble();
      double z = coords.size() > 2 ? coords.get(2).asDouble() : 0.0;

      points.add(new LasPointGeometry(x, y, z, LidarClass.BATIMENT));
    }

    return points;
  }
}
