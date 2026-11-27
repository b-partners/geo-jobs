package app.bpartners.geojobs.endpoint.rest.postprocessing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.postprocessing.Geojson;
import app.bpartners.geojobs.postprocessing.model.LatLonPolygon;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;

class GeojsonTest {
  private static final String POLYGON_RING = "[[0,0],[10,0],[10,10],[0,10],[0,0]]";
  private static final String OTHER_POLYGON_RING = "[[20,20],[30,20],[30,30],[20,30],[20,20]]";

  @TempDir Path tempDir;
  ObjectMapper objectMapper = new ObjectMapper();

  @SneakyThrows
  private File geojsonFile(String geometryType, String coordinates, String properties) {
    var content =
        """
        {"type":"FeatureCollection","features":[
          {"type":"Feature","properties":%s,"geometry":{"type":"%s","coordinates":%s}}
        ]}
        """
            .formatted(properties, geometryType, coordinates);
    return Files.writeString(tempDir.resolve("input.geojson"), content).toFile();
  }

  private static Coordinate[] coordinates(double... xy) {
    var coordinates = new Coordinate[xy.length / 2];
    for (int i = 0; i < coordinates.length; i++) {
      coordinates[i] = new Coordinate(xy[2 * i], xy[2 * i + 1]);
    }
    return coordinates;
  }

  private Polygon polygon(Map<String, String> userData, double... shell) {
    var polygon = new GeometryFactory().createPolygon(coordinates(shell));
    polygon.setUserData(userData);
    return polygon;
  }

  @Test
  void reads_polygon_with_label_and_confidence_from_file() {
    var file =
        geojsonFile(
            "Polygon", "[" + POLYGON_RING + "]", "{\"label\":\"tombe\",\"confidence\":0.9}");

    var actual = new Geojson(file);

    assertEquals(1, actual.polygons().size());
    var polygon = actual.polygons().iterator().next().polygon();
    var userData = (Map<?, ?>) polygon.getUserData();
    assertEquals("tombe", userData.get("label"));
    assertEquals(0.9, userData.get("confidence"));
    assertEquals(100.0, polygon.getArea());
  }

  @Test
  void reads_polygon_without_confidence_as_null_confidence() {
    var file = geojsonFile("Polygon", "[" + POLYGON_RING + "]", "{\"label\":\"tombe\"}");

    var actual = new Geojson(file);

    var userData = (Map<?, ?>) actual.polygons().iterator().next().polygon().getUserData();
    assertEquals("tombe", userData.get("label"));
    assertNull(userData.get("confidence"));
  }

  @Test
  void reads_multi_polygon_made_of_a_single_polygon() {
    var file =
        geojsonFile(
            "MultiPolygon", "[[" + POLYGON_RING + "]]", "{\"label\":\"arbre\",\"confidence\":0.5}");

    var actual = new Geojson(file);

    assertEquals(1, actual.polygons().size());
    assertEquals(100.0, actual.polygons().iterator().next().polygon().getArea());
  }

  @Test
  void rejects_multi_polygon_made_of_several_polygons() {
    var file =
        geojsonFile(
            "MultiPolygon",
            "[[" + POLYGON_RING + "],[" + OTHER_POLYGON_RING + "]]",
            "{\"label\":\"arbre\",\"confidence\":0.5}");

    var error = assertThrows(RuntimeException.class, () -> new Geojson(file));

    assertTrue(error.getMessage().startsWith("Only mulitpolygons with single polygon supported"));
  }

  @Test
  void rejects_missing_file() {
    var missingFile = tempDir.resolve("missing.geojson").toFile();

    assertThrows(RuntimeException.class, () -> new Geojson(missingFile));
  }

  @Test
  @SneakyThrows
  void string_value_is_a_feature_collection_with_swapped_lat_lon_and_properties() {
    var userData = Map.of("label", "tombe", "confidence", "0.9");
    var polygon = polygon(userData, 1, 2, 5, 2, 5, 6, 1, 6, 1, 2);

    var actual = new Geojson(Set.of(new LatLonPolygon(polygon)));

    JsonNode json = objectMapper.readTree(actual.stringValue());
    assertEquals("FeatureCollection", json.get("type").asText());
    var feature = json.get("features").get(0);
    assertEquals("Polygon", feature.get("geometry").get("type").asText());
    assertEquals("tombe", feature.get("properties").get("label").asText());
    var firstPoint = feature.get("geometry").get("coordinates").get(0).get(0);
    assertEquals(2.0, firstPoint.get(0).asDouble());
    assertEquals(1.0, firstPoint.get(1).asDouble());
  }

  @Test
  @SneakyThrows
  void string_value_keeps_polygon_holes_as_additional_rings() {
    var factory = new GeometryFactory();
    var shell = factory.createLinearRing(coordinates(0, 0, 10, 0, 10, 10, 0, 10, 0, 0));
    var hole = factory.createLinearRing(coordinates(2, 2, 4, 2, 4, 4, 2, 4, 2, 2));
    var polygon = factory.createPolygon(shell, new LinearRing[] {hole});
    polygon.setUserData(Map.of("label", "tombe"));

    var actual = new Geojson(Set.of(new LatLonPolygon(polygon)));

    var rings =
        objectMapper
            .readTree(actual.stringValue())
            .get("features")
            .get(0)
            .get("geometry")
            .get("coordinates");
    assertEquals(2, rings.size());
    assertEquals(5, rings.get(1).size());
  }

  @Test
  @SneakyThrows
  void save_as_file_writes_string_value() {
    var polygon = polygon(Map.of("label", "tombe"), 0, 0, 10, 0, 10, 10, 0, 10, 0, 0);
    var subject = new Geojson(Set.of(new LatLonPolygon(polygon)));
    var outputFile = tempDir.resolve("output.geojson");

    subject.saveAsFile(outputFile.toString());

    assertEquals(subject.stringValue(), Files.readString(outputFile));
  }

  @Test
  void save_as_file_fails_when_output_directory_does_not_exist() {
    var polygon = polygon(Map.of("label", "tombe"), 0, 0, 10, 0, 10, 10, 0, 10, 0, 0);
    var subject = new Geojson(Set.of(new LatLonPolygon(polygon)));
    var unreachablePath = tempDir.resolve("missing-dir").resolve("output.geojson").toString();

    assertThrows(RuntimeException.class, () -> subject.saveAsFile(unreachablePath));
  }
}
