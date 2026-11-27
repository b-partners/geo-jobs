package app.bpartners.geojobs.service.ign;

import static app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.MULTI_POLYGON;
import static app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.POLYGON;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class IgnCadastreFeatureFetcherTest {
  private static final String IGN_URL_PREFIX =
      "https://apicarto.ign.fr/api/cadastre/parcelle?geom=";
  private static final GeometryFactory FACTORY = new GeometryFactory();

  RestTemplate restTemplate = new RestTemplate();
  MockRestServiceServer server;
  IgnCadastreFeatureFetcher subject = new IgnCadastreFeatureFetcher(restTemplate);

  @BeforeEach
  void setup() {
    server = MockRestServiceServer.bindTo(restTemplate).build();
  }

  private static Geometry square() {
    return FACTORY.createPolygon(
        new Coordinate[] {
          new Coordinate(2.0, 48.0),
          new Coordinate(2.1, 48.0),
          new Coordinate(2.1, 48.1),
          new Coordinate(2.0, 48.1),
          new Coordinate(2.0, 48.0)
        });
  }

  private void respondWith(String body) {
    server
        .expect(requestTo(startsWith(IGN_URL_PREFIX)))
        .andExpect(method(GET))
        .andRespond(withSuccess(body, APPLICATION_JSON));
  }

  @Test
  void multi_polygon_parcel_is_mapped_to_feature_with_properties() {
    respondWith(
        """
{"type":"FeatureCollection","features":[
  {"type":"Feature","properties":{"numero":"12","section":"AB"},
   "geometry":{"type":"MultiPolygon","coordinates":[[[[2,48],[2.1,48],[2.1,48.1],[2,48]]]]}}
]}
""");

    var actual = subject.apply(square());

    assertEquals(1, actual.size());
    assertEquals(MULTI_POLYGON, actual.getFirst().getGeometry().getGeometryType());
    assertTrue(
        actual.getFirst().getGeometry().getActualInstanceStringValue().contains("MultiPolygon"));
    assertEquals(Map.of("numero", "12", "section", "AB"), actual.getFirst().getProperties());
    server.verify();
  }

  @Test
  void polygon_parcel_without_properties_gets_empty_properties() {
    respondWith(
        """
        {"features":[
          {"geometry":{"type":"Polygon","coordinates":[[[2,48],[2.1,48],[2.1,48.1],[2,48]]]}},
          {"properties":{"numero":"13"},
           "geometry":{"type":"Polygon","coordinates":[[[2,48],[2.1,48],[2.1,48.1],[2,48]]]}}
        ]}
        """);

    var actual = subject.apply(square());

    assertEquals(2, actual.size());
    assertEquals(POLYGON, actual.getFirst().getGeometry().getGeometryType());
    assertEquals(Map.of(), actual.getFirst().getProperties());
    assertEquals(Map.of("numero", "13"), actual.getLast().getProperties());
  }

  @Test
  void point_is_searched_through_a_bounding_box_around_it() {
    server
        .expect(requestTo(startsWith(IGN_URL_PREFIX)))
        .andExpect(requestTo(containsString("Polygon")))
        .andRespond(
            withSuccess(
                """
{"features":[
  {"geometry":{"type":"Polygon","coordinates":[[[2,48],[2.1,48],[2.1,48.1],[2,48]]]}}
]}
""",
                APPLICATION_JSON));
    var point = FACTORY.createPoint(new Coordinate(2.05, 48.05));

    var actual = subject.apply(point);

    assertEquals(1, actual.size());
    server.verify();
  }

  @Test
  void empty_features_are_rejected() {
    respondWith("{\"features\":[]}");
    var geometry = square();

    var error = assertThrows(IllegalStateException.class, () -> subject.apply(geometry));

    assertTrue(error.getMessage().contains("empty features obtained"));
  }

  @Test
  void missing_features_are_rejected() {
    respondWith("{}");
    var geometry = square();

    assertThrows(IllegalStateException.class, () -> subject.apply(geometry));
  }

  @Test
  void parcel_with_unsupported_geometry_type_is_rejected() {
    respondWith(
        """
        {"features":[{"geometry":{"type":"Point","coordinates":[2,48]}}]}
        """);
    var geometry = square();

    var error = assertThrows(UnsupportedOperationException.class, () -> subject.apply(geometry));

    assertTrue(error.getMessage().startsWith("Unable to retrieve GeometryType"));
  }
}
