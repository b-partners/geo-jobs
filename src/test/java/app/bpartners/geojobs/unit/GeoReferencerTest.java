package app.bpartners.geojobs.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import app.bpartners.geojobs.service.geojson.GeoReferencer;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class GeoReferencerTest {
  List<BigDecimal> expectedCoordinates() {
    return List.of(BigDecimal.valueOf(4.8996671020745985), BigDecimal.valueOf(45.82147342781088));
  }

  @Test
  void geographical_coordinates_from_tile_ok() {
    var actual =
        GeoReferencer.toGeographicalCoordinates(
            538559, 373791, 322.36584784164086, 385.8621512043883, 20, 1024);

    assertNotNull(actual);
    assertEquals(expectedCoordinates(), actual);
  }
}
