package app.bpartners.geojobs.endpoint.rest.postprocessing;

import static java.io.File.createTempFile;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GeoJsonValidatorTest {
  private GeoJsonValidator subject;

  @BeforeEach
  void setUp() {
    var mapper = new ObjectMapper();
    subject = new GeoJsonValidator(mapper);
  }

  @Test
  void test_an_invalid_geoJSON_without_polygon_enclosure() throws IOException {
    var invalidGeoJson =
        """
        {
          "type": "FeatureCollection",
          "features": [
            {
              "type": "Feature",
              "geometry": {
                "type": "Polygon",
                "coordinates": [
                  [
                    [0.0, 0.0],
                    [1.0, 0.0],
                    [1.0, 1.0],
                    [0.0, 1.0]
                  ]
                ]
              },
              "properties": {}
            }
          ]
        }
        """;
    assertCheckInvalidGeoJson(invalidGeoJson);
  }

  @Test
  void test_an_invalid_geoJSON_with_self_intersection() throws IOException {
    var invalidGeoJSONContent =
        """
        {
          "type": "FeatureCollection",
          "features": [
            {
              "type": "Feature",
              "geometry": {
                "type": "Polygon",
                "coordinates": [
                  [
                    [0.0, 0.0],
                    [2.0, 2.0],
                    [0.0, 2.0],
                    [2.0, 0.0],
                    [0.0, 0.0]
                  ]
                ]
              },
              "properties": {
                "name": "Self-intersecting polygon"
              }
            }
          ]
        }

        """;
    assertCheckInvalidGeoJson(invalidGeoJSONContent);
  }

  @Test
  void test_an_invalid_geoJSON_with_overlapping_outer_shell() throws IOException {
    var invalidJSON =
        """
        {
          "type": "Feature",
          "geometry": {
            "type": "Polygon",
            "coordinates": [
              [
                [0.0, 0.0],
                [4.0, 0.0],
                [4.0, 4.0],
                [0.0, 4.0],
                [0.0, 0.0]
              ],
              [
                [2.0, 2.0],
                [5.0, 2.0],
                [5.0, 5.0],
                [2.0, 5.0],
                [2.0, 2.0]
              ]
            ]
          },
          "properties": {
            "name": "Hole overlapping outer shell"
          }
        }

        """;
    assertCheckInvalidGeoJson(invalidJSON);
  }

  @Test
  void test_an_invalid_geoJSON_with_wrong_dimension() throws IOException {
    var invalidJSON =
        """
        {
          "type": "Feature",
          "geometry": {
            "type": "Polygon",
            "coordinates": [
              [
                [0.0, 0.0],
                [2.0, 0.0, 10.0],
                [2.0, 2.0],
                [0.0, 2.0],
                [0.0, 0.0]
              ]
            ]
          },
          "properties": {
            "name": "Dimension mismatch"
          }
        }

        """;
    assertCheckInvalidGeoJson(invalidJSON);
  }

  @Test
  void test_a_valid_geojson() throws URISyntaxException {
    var resource = getClass().getResource("/geojson/quai-de-bourbon.geojson");
    assertNotNull(resource);
    File geoJSON = new File(resource.toURI());

    assertTrue(subject.test(geoJSON));
  }

  private void assertCheckInvalidGeoJson(String geoJson) throws IOException {
    var invalidGeoJSON = getInvalidGeoJSONFile(geoJson);
    assertThrows(RuntimeException.class, () -> subject.test(invalidGeoJSON));
  }

  private File getInvalidGeoJSONFile(String invalidContent) throws IOException {
    var invalidF = createTempFile("invalid" + UUID.randomUUID(), ".geojson");
    Files.writeString(invalidF.toPath(), invalidContent);
    return invalidF;
  }
}
