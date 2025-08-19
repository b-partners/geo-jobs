package app.bpartners.geojobs.endpoint.rest.postprocessing;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class GeoJsonValidatorTest {
  ObjectMapper objectMapper = new ObjectMapper();
  GeoJsonValidator geoJsonValidator = new GeoJsonValidator(objectMapper);

    private File createTempFileTest(String geojsonContent, String fileName) throws IOException {
        var suffix = ".geojson";
        var tempFileTest = File.createTempFile(fileName, suffix);

        try (var writer = new FileWriter(tempFileTest)) {
            writer.write(geojsonContent);
        }

        tempFileTest.deleteOnExit();
        return tempFileTest;
    }

  @Test
  public void read_file_returns_expected_jsonnode() throws IOException {
    var json = "{\"type\":\"FeatureCollection\",\"features\":[]}";
    var file = createTempFileTest(json, "expected_geojson");
    var parsedNode = geoJsonValidator.readFile(file);

    assertEquals("FeatureCollection", parsedNode.get("type").asText());
    assertTrue(parsedNode.get("features").isArray());
  }

  @Test
  public void valid_geojson_return_true() throws IOException {
    var ivandryGeojson =
        new File(getClass().getResource("/ivandry/route-ivandry.geojson").getFile());

      assertTrue(geoJsonValidator.isValid(ivandryGeojson));
  }

  @Test
  public void empty_file_returns_false() throws IOException {
        var file = createTempFileTest("", "empty_geojson");
    assertFalse(geoJsonValidator.isValid(file));
  }

  @Test
  public void invalid_type_returns_false() throws IOException {
    var invalidTypeJson = "{\"type\":\"InvalidType\",\"features\":[]}";
      var file = createTempFileTest(invalidTypeJson, "expected_geojson");

    assertFalse(geoJsonValidator.isValid(file));
  }

  @Test
  public void empty_features_returns_false() throws IOException {
    var emptyFeaturesJson = "{\"type\":\"FeatureCollection\",\"features\":[]}";
      var file = createTempFileTest(emptyFeaturesJson, "expected_geojson");

    assertFalse(geoJsonValidator.isValid(file));
  }

  @Test
  public void invalid_feature_type_returns_false() throws IOException {
    var invalidFeatureJson =
        "{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"InvalidType\"}]}";
      var file = createTempFileTest(invalidFeatureJson, "expected_geojson");

    assertFalse(geoJsonValidator.isValid(file));
  }

  @Test
  public void missing_geometry_returns_false() throws IOException {
    var missingGeometryJson =
        "{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\"}]}";
      var file = createTempFileTest(missingGeometryJson, "expected_geojson");

    assertFalse(geoJsonValidator.isValid(file));
  }

  @Test
  public void invalid_geometry_type_returns_false() throws IOException {
    var invalidGeometryJson =
        "{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\"}}]}";
      var file = createTempFileTest(invalidGeometryJson, "expected_geojson");

    assertFalse(geoJsonValidator.isValid(file));
  }

  @Test
  public void invalid_coordinates_structure_returns_false() throws IOException {
    var invalidCoordsJson =
        "{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"geometry\":{\"type\":\"Polygon\",\"coordinates\":\"invalid\"}}]}";

      var file = createTempFileTest(invalidCoordsJson, "expected_geojson");

    assertFalse(geoJsonValidator.isValid(file));
  }

  @Test
  public void invalid_polygon_coordinates_returns_false() throws IOException {
    var invalidPolygonJson =
        "{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[0,0],[1,1]]]}}]}";
      var file = createTempFileTest(invalidPolygonJson, "expected_geojson");

    assertFalse(geoJsonValidator.isValid(file));
  }

  @Test
  public void invalid_lat_lon_values_returns_false() throws IOException {
    var invalidLatLonJson =
        "{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[200,100],[100,200],[200,100]]]}}]}";

      var file = createTempFileTest(invalidLatLonJson, "expected_geojson");
    assertFalse(geoJsonValidator.isValid(file));
  }
}
