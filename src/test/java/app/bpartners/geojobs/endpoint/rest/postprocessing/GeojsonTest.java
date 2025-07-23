package app.bpartners.geojobs.endpoint.rest.postprocessing;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

class GeojsonTest {

  @SneakyThrows
  @Test
  void should_parse_geojson_file_to_geojson_object() {
    var resource = getClass().getResource("/geojson/avenue-pierre-grenier.geojson");
    assertNotNull(resource);

    File geojsonFile = new File(resource.toURI());
    assertTrue(geojsonFile.exists());

    var geojson = new Geojson(geojsonFile);
    assertNotNull(geojson);
    assertFalse(geojson.polygons().isEmpty());
  }
}
