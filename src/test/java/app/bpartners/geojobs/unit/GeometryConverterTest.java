package app.bpartners.geojobs.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import app.bpartners.geojobs.endpoint.rest.model.Point;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import app.bpartners.geojobs.service.gouv.fr.rnb.BuildingApi;
import java.math.BigDecimal;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class GeometryConverterTest {
  BuildingApi buildingApiMock = mock();
  GeometryConverter subject = new GeometryConverter(buildingApiMock);

  @Test
  void convert_using_70_meters_x_70_meters_in_size() {
    var latitude = BigDecimal.valueOf(46.651930);
    var longitude = BigDecimal.valueOf(-0.249317);
    var sizeInMeters = 70.0;

    var actual = subject.apply(new Point().coordinates(List.of(longitude, latitude)), sizeInMeters);
    var actualString = subject.writeMultiPolygonAsString(actual);

    assertEquals(expectedMultiPolygonStringValue(), actualString);
  }

  public String expectedMultiPolygonStringValue() {
    return """
{"type":"MultiPolygon","coordinates":[[[[-0.249775035790335,46.65161559108876],[-0.248858964209665,46.65161559108876],[-0.248858964209665,46.65224440891125],[-0.249775035790335,46.65224440891125],[-0.249775035790335,46.65161559108876]]]]}""";
  }
}
