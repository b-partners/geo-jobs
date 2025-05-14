package app.bpartners.geojobs.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.gen.annotator.endpoint.rest.model.Point;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class GeometryConverterTest {
  GeometryConverter subject = new GeometryConverter();

  @Test
  void convert_using_70_meters_x_70_meters_in_size() {
    double latitude = 46.651930;
    double longitude = -0.249317;
    var sizeInMeters = 70.0;

    var actual = subject.apply(new Point().x(latitude).y(longitude), sizeInMeters);
    var actualString = subject.writeMultiPolygonAsString(actual);

    assertEquals(expectedMultiPolygonStringValue(), actualString);
  }

  public String expectedMultiPolygonStringValue() {
    return """
{"type":"MultiPolygon","coordinates":[[[[-0.249775035790335,46.65161559108876],[-0.248858964209665,46.65161559108876],[-0.248858964209665,46.65224440891125],[-0.249775035790335,46.65224440891125],[-0.249775035790335,46.65161559108876]]]]}""";
  }
}
