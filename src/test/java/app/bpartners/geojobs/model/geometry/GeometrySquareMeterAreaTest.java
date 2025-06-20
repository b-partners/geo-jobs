package app.bpartners.geojobs.model.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.service.GeometrySquareMeterArea;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

class GeometrySquareMeterAreaTest {
  GeometrySquareMeterArea subject = new GeometrySquareMeterArea();

  @SneakyThrows
  @Test
  void geometry_area() {
    var feature = new ObjectMapper().readValue(feature(), Feature.class);
    var geometry =
        new GeometryConverter(null).apply(feature.getGeometry().getMultiPolygon().getCoordinates());

    var actual = subject.apply(geometry);

    assertEquals(10512.793872714043, actual);
  }

  private String feature() {
    return """
           {
                       "type": "Feature",
                       "geometry": {
                           "coordinates": [
                               [
                                   [
                                       [
                                           0.6754183664424431,
                                           47.38125018586146
                                       ],
                                       [
                                           0.6754183664424431,
                                           47.38013583016112
                                       ],
                                       [
                                           0.6765420562064719,
                                           47.38013583016112
                                       ],
                                       [
                                           0.6765420562064719,
                                           47.38125018586146
                                       ],
                                       [
                                           0.6754183664424431,
                                           47.38125018586146
                                       ]
                                   ]
                               ]
                           ],
                           "type": "MultiPolygon"
                       },
                       "properties": {}
                   }""";
  }
}
