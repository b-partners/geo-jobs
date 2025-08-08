package app.bpartners.geojobs.endpoint.rest.postprocessing;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

public class GeoJsonValidatorTest {
  ObjectMapper objectMapper = new ObjectMapper();
  GeoJsonValidator geoJsonValidator = new GeoJsonValidator(objectMapper);

  @Test
  public void valid_geojson_return_true() throws IOException {
    var ivandryGeojson =
        new File(getClass().getResource("/ivandry/route-ivandry.geojson").getFile());

    try (var inputStream = new FileInputStream(ivandryGeojson)) {
      MultipartFile file =
          new MockMultipartFile("file", ivandryGeojson.getName(), "application/json", inputStream);

      assertTrue(geoJsonValidator.isValid(file));
    }
  }

  @Test
  public void empty_file_returns_false() {
    var file = new MockMultipartFile("file", "empty.geojson", "application/json", new byte[0]);
    assertFalse(geoJsonValidator.isValid(file));
  }

  @Test
  public void invalid_type_returns_false() {
    String invalidTypeJson = "{\"type\":\"InvalidType\",\"features\":[]}";
    var file =
        new MockMultipartFile(
            "file", "invalid.geojson", "application/json", invalidTypeJson.getBytes());
    assertFalse(geoJsonValidator.isValid(file));
  }

  @Test
  public void empty_features_returns_false() {
    String emptyFeaturesJson = "{\"type\":\"FeatureCollection\",\"features\":[]}";
    var file =
        new MockMultipartFile(
            "file", "empty_features.geojson", "application/json", emptyFeaturesJson.getBytes());
    assertFalse(geoJsonValidator.isValid(file));
  }

  @Test
  public void invalid_feature_type_returns_false() {
    String invalidFeatureJson =
        "{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"InvalidType\"}]}";
    var file =
        new MockMultipartFile(
            "file", "invalid_feature.geojson", "application/json", invalidFeatureJson.getBytes());
    assertFalse(geoJsonValidator.isValid(file));
  }

  @Test
  public void missing_geometry_returns_false() {
    String missingGeometryJson =
        "{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\"}]}";
    var file =
        new MockMultipartFile(
            "file", "missing_geometry.geojson", "application/json", missingGeometryJson.getBytes());
    assertFalse(geoJsonValidator.isValid(file));
  }

  @Test
  public void invalid_geometry_type_returns_false() {
    String invalidGeometryJson =
        "{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\"}}]}";
    var file =
        new MockMultipartFile(
            "file", "invalid_geometry.geojson", "application/json", invalidGeometryJson.getBytes());
    assertFalse(geoJsonValidator.isValid(file));
  }

  @Test
  public void invalid_coordinates_structure_returns_false() {
    String invalidCoordsJson =
        "{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"geometry\":{\"type\":\"Polygon\",\"coordinates\":\"invalid\"}}]}";
    var file =
        new MockMultipartFile(
            "file", "invalid_coords.geojson", "application/json", invalidCoordsJson.getBytes());
    assertFalse(geoJsonValidator.isValid(file));
  }

  @Test
  public void invalid_polygon_coordinates_returns_false() {
    String invalidPolygonJson =
        "{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[0,0],[1,1]]]}}]}";
    var file =
        new MockMultipartFile(
            "file", "invalid_polygon.geojson", "application/json", invalidPolygonJson.getBytes());
    assertFalse(geoJsonValidator.isValid(file));
  }

  @Test
  public void invalid_lat_lon_values_returns_false() {
    String invalidLatLonJson =
        "{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[200,100],[100,200],[200,100]]]}}]}";
    var file =
        new MockMultipartFile(
            "file", "invalid_latlon.geojson", "application/json", invalidLatLonJson.getBytes());
    assertFalse(geoJsonValidator.isValid(file));
  }
}
