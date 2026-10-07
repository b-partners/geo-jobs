package app.bpartners.geojobs.service.threed.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

class CityJsonRoofMetricsTest {

  @Test
  void slope_of_largest_roof_and_height_from_ground() {
    var actual =
        CityJsonRoofMetrics.from(
            json(
                """
{
  "transform": {"scale": [0.01, 0.01, 0.01], "translate": [0, 0, 100]},
  "vertices": [[0, 0, 500], [100, 0, 500], [0, 100, 800],
               [0, 0, 0], [100, 0, 0], [0, 100, 0]],
  "CityObjects": {"0": {"geometry": [{
    "type": "MultiSurface",
    "boundaries": [[[0, 1, 2]], [[0, 1, 2]], [[3, 4, 5]]],
    "semantics": {
      "surfaces": [
        {"type": "RoofSurface", "slope_in_degrees": 10, "area_in_square_meters": 5},
        {"type": "RoofSurface", "slope_in_degrees": 35, "area_in_square_meters": 50},
        {"type": "GroundSurface"}
      ],
      "values": [0, 1, 2]
    }
  }]}}
}
"""));

    assertTrue(actual.hasRoof());
    assertEquals(35, actual.slopeInDegrees());
    assertEquals(8, actual.heightInMeters());
  }

  @Test
  void no_roof_when_cityjson_has_no_building() {
    var actual = CityJsonRoofMetrics.from(json("{\"CityObjects\": {}, \"vertices\": []}"));

    assertFalse(actual.hasRoof());
    assertEquals(0, actual.slopeInDegrees());
    assertEquals(0, actual.heightInMeters());
  }

  @SneakyThrows
  private static com.fasterxml.jackson.databind.JsonNode json(String value) {
    return new ObjectMapper().readTree(value);
  }
}
