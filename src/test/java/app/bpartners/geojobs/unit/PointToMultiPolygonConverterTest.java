package app.bpartners.geojobs.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.gen.annotator.endpoint.rest.model.Point;
import app.bpartners.geojobs.service.geojson.PointToMultiPolygonConverter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class PointToMultiPolygonConverterTest {
  PointToMultiPolygonConverter subject = new PointToMultiPolygonConverter();

  @Test
  void convert_using_70_meters_x_70_meters_in_size() {
    var latitude = 48.8589892;
    var longitude = 2.2847458;
    var sizeInMeters = 70.0;

    var actual = subject.apply(new Point().x(latitude).y(longitude), sizeInMeters);
    var actualString = subject.generateSquareMultiPolygon(actual);

    assertEquals(expectedMultiPolygonStringValue(), actualString);
  }

  public String expectedMultiPolygonStringValue() {
    return """
{"type":"MultiPolygon","coordinates":[[[[2.2843,48.8587],[2.2852,48.8587],[2.2852,48.8593],[2.2843,48.8593],[2.2843,48.8587]]]]}""";
  }
}
