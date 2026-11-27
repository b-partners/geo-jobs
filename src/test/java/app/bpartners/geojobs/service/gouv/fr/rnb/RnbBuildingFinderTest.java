package app.bpartners.geojobs.service.gouv.fr.rnb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import app.bpartners.geojobs.endpoint.rest.model.Point;
import app.bpartners.geojobs.model.exception.BuildingFootPrintSourceUnavailableException;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.service.PolygonInsideCircleDistanceComputer;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import app.bpartners.geojobs.service.google.maps.GeoCodeApi;
import app.bpartners.geojobs.service.google.maps.GeoPosition;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class RnbBuildingFinderTest {
  private static final String RNB = "https://rnb-api.beta.gouv.fr/api/alpha/buildings/";
  private static final String SQUARE_POLYGON =
      "{\"type\":\"Polygon\",\"coordinates\":[[[2,48],[2.001,48],[2.001,48.001],[2,48.001],[2,48]]]}";
  private static final String SQUARE_MULTI_POLYGON =
      "{\"type\":\"MultiPolygon\",\"coordinates\":[[[[2,48],[2.001,48],[2.001,48.001],[2,48.001],[2,48]]]]}";
  private static final String FAR_POLYGON =
      "{\"type\":\"Polygon\",\"coordinates\":[[[10,40],[10.001,40],[10.001,40.001],[10,40.001],[10,40]]]}";
  private static final String POINT_SHAPE = "{\"type\":\"Point\",\"coordinates\":[2.0005,48.0005]}";

  GeometryConverter geometryConverter = new GeometryConverter();
  PolygonInsideCircleDistanceComputer circleDistanceComputerMock = mock();
  GeoCodeApi geoCodeApiMock = mock();
  RnbBuildingFinder subject =
      new RnbBuildingFinder(geometryConverter, circleDistanceComputerMock, geoCodeApiMock);
  MockRestServiceServer server;

  @BeforeEach
  void setup() {
    var restTemplate = new RestTemplate();
    ReflectionTestUtils.setField(subject, "restTemplate", restTemplate);
    server = MockRestServiceServer.bindTo(restTemplate).build();
  }

  private static String building(String rnbId, String shape, Double distance, String addresses) {
    return """
{"rnb_id":"%s","status":"constructed","point":%s,"shape":%s,"addresses":%s,"distance":%s}
"""
        .formatted(rnbId, POINT_SHAPE, shape, addresses == null ? "[]" : addresses, distance);
  }

  private void expectClosest(String resultsJson, String nextUrl) {
    server
        .expect(requestTo(Matchers.startsWith(RNB + "closest/")))
        .andExpect(method(GET))
        .andExpect(queryParam("from", "tech@birdia.fr"))
        .andRespond(
            withSuccess(
                "{\"next\":%s,\"results\":%s}"
                    .formatted(nextUrl == null ? "null" : "\"" + nextUrl + "\"", resultsJson),
                APPLICATION_JSON));
  }

  private void expectBuilding(String rnbId, String buildingJson) {
    server
        .expect(requestTo(RNB + rnbId + "/"))
        .andExpect(method(GET))
        .andRespond(withSuccess(buildingJson, APPLICATION_JSON));
  }

  private static List<List<BigDecimal>> providedRing() {
    return List.of(
        List.of(BigDecimal.valueOf(1.9995), BigDecimal.valueOf(47.9995)),
        List.of(BigDecimal.valueOf(2.0015), BigDecimal.valueOf(47.9995)),
        List.of(BigDecimal.valueOf(2.0015), BigDecimal.valueOf(48.0015)),
        List.of(BigDecimal.valueOf(1.9995), BigDecimal.valueOf(48.0015)),
        List.of(BigDecimal.valueOf(1.9995), BigDecimal.valueOf(47.9995)));
  }

  @Test
  void closest_buildings_are_requested_around_the_point_with_the_radius() {
    server
        .expect(requestTo(Matchers.startsWith(RNB + "closest/")))
        .andExpect(queryParam("radius", "50"))
        .andExpect(queryParam("point", "48.0,2.0"))
        .andExpect(queryParam("from", "tech@birdia.fr"))
        .andRespond(
            withSuccess(
                "{\"next\":null,\"results\":[%s]}"
                    .formatted(building("RNB-1", SQUARE_POLYGON, 3.5, null)),
                APPLICATION_JSON));

    var actual = subject.getBuildingClosest(48.0, 2.0, 50);

    assertEquals(1, actual.results().size());
    assertEquals("RNB-1", actual.results().getFirst().rnbId());
    assertEquals(3.5, actual.results().getFirst().distance());
    server.verify();
  }

  @Test
  void next_page_is_requested_from_its_decoded_url_with_the_contact_parameter() {
    server
        .expect(requestTo(Matchers.startsWith(RNB + "closest/?page=2")))
        .andExpect(queryParam("from", "tech@birdia.fr"))
        .andRespond(withSuccess("{\"results\":[]}", APPLICATION_JSON));

    var actual = subject.getBuildingByNextUrl(RNB + "closest/%3Fpage=2".replace("%3F", "?"));

    assertTrue(actual.results().isEmpty());
    server.verify();
  }

  @Test
  void nearest_building_combines_closest_point_and_distance_with_the_building_details() {
    expectClosest(
        "[%s,%s]"
            .formatted(
                building("FAR", SQUARE_POLYGON, 40.0, null),
                building("NEAR", SQUARE_POLYGON, 4.0, null)),
        null);
    expectBuilding("NEAR", building("NEAR", SQUARE_MULTI_POLYGON, null, "[{\"street\":\"rue\"}]"));

    var actual = subject.getNearestBuildingAt(2.0, 48.0, 100);

    assertEquals("NEAR", actual.rnbId());
    assertEquals(4.0, actual.distance());
    assertEquals(1, actual.addresses().size());
    assertEquals("rue", actual.addresses().getFirst().street());
    server.verify();
  }

  @Test
  void nearest_building_is_not_found_when_nothing_is_around() {
    expectClosest("[]", null);

    var error =
        assertThrows(NotFoundException.class, () -> subject.getNearestBuildingAt(2.0, 48.0, 100));

    assertTrue(error.getMessage().contains("No building found at point"));
  }

  @Test
  void roof_polygons_are_built_from_buildings_inside_the_provided_polygon_with_addresses() {
    when(circleDistanceComputerMock.apply(any(), any())).thenReturn(150.0);
    expectClosest(
        "[%s]".formatted(building("RNB-1", SQUARE_POLYGON, 1.0, null)), RNB + "closest/?page=2");
    server
        .expect(requestTo(Matchers.startsWith(RNB + "closest/?page=2")))
        .andRespond(
            withSuccess(
                "{\"next\":null,\"results\":[%s,%s,%s]}"
                    .formatted(
                        building("RNB-2", SQUARE_MULTI_POLYGON, 2.0, null),
                        building("RNB-FAR", FAR_POLYGON, 2.0, null),
                        building("RNB-POINT", POINT_SHAPE, 2.0, null)),
                APPLICATION_JSON));
    var addresses =
        """
        [{"street_number":"12","street_rep":"bis","street":"rue de la Paix",
          "city_name":"Paris","city_zipcode":"75002"},
         {"street_number":"3","street_rep":"","street":"avenue Foch"},
         {"city_name":"Lyon"}]
        """;
    expectBuilding("RNB-1", building("RNB-1", SQUARE_POLYGON, null, addresses));
    expectBuilding("RNB-2", building("RNB-2", SQUARE_MULTI_POLYGON, null, null));
    expectBuilding("RNB-FAR", building("RNB-FAR", FAR_POLYGON, null, null));
    expectBuilding("RNB-POINT", building("RNB-POINT", POINT_SHAPE, null, null));

    var actual = subject.retrieveRoofPolygonsFrom(providedRing());

    assertEquals(2, actual.size());
    assertEquals(
        List.of("12 bis rue de la Paix Paris 75002", "3 avenue Foch", "Lyon"),
        actual.getFirst().addresses());
    assertEquals(List.of(), actual.getLast().addresses());
    assertTrue(actual.stream().allMatch(roof -> roof.latLonGeometry() != null));
    server.verify();
  }

  @Test
  void roof_polygons_of_a_zone_larger_than_the_supported_radius_are_rejected() {
    when(circleDistanceComputerMock.apply(any(), any())).thenReturn(2_000.0);
    var ring = providedRing();

    var error =
        assertThrows(
            UnsupportedOperationException.class, () -> subject.retrieveRoofPolygonsFrom(ring));

    assertTrue(error.getMessage().contains("larger than supported"));
  }

  @Test
  void roof_polygons_are_reported_unavailable_when_rnb_fails() {
    when(circleDistanceComputerMock.apply(any(), any())).thenReturn(150.0);
    server
        .expect(requestTo(Matchers.startsWith(RNB + "closest/")))
        .andRespond(withStatus(INTERNAL_SERVER_ERROR));
    var ring = providedRing();

    assertThrows(
        BuildingFootPrintSourceUnavailableException.class,
        () -> subject.retrieveRoofPolygonsFrom(ring));
  }

  @Test
  void building_multi_polygon_of_missing_point_is_null() {
    assertNull(subject.getBuildingMultiPolygon((Point) null));
  }

  @Test
  void building_multi_polygon_from_coordinates_is_the_nearest_building_shape() {
    expectClosest("[%s]".formatted(building("RNB-1", SQUARE_MULTI_POLYGON, 1.0, null)), null);
    expectBuilding("RNB-1", building("RNB-1", SQUARE_MULTI_POLYGON, null, null));

    var actual =
        subject.getBuildingMultiPolygon(
            new Point().coordinates(List.of(BigDecimal.valueOf(2.0), BigDecimal.valueOf(48.0))));

    assertEquals(1, actual.getNumGeometries());
    server.verify();
  }

  @Test
  void building_multi_polygon_from_address_geocodes_the_address_first() throws Exception {
    when(geoCodeApiMock.searchGeoPositionFromAddress("1 rue de Paris"))
        .thenReturn(new GeoPosition(48.0, 2.0));
    expectClosest("[%s]".formatted(building("RNB-1", SQUARE_MULTI_POLYGON, 1.0, null)), null);
    expectBuilding("RNB-1", building("RNB-1", SQUARE_MULTI_POLYGON, null, null));

    var actual = subject.getBuildingMultiPolygon("1 rue de Paris");

    assertEquals(1, actual.getNumGeometries());
  }

  @Test
  void building_multi_polygon_from_an_address_that_cannot_be_geocoded_is_rejected()
      throws Exception {
    when(geoCodeApiMock.searchGeoPositionFromAddress("nowhere"))
        .thenThrow(new IOException("geocoder down"));

    var error =
        assertThrows(RuntimeException.class, () -> subject.getBuildingMultiPolygon("nowhere"));

    assertTrue(error.getMessage().contains("Unable to geocode address nowhere"));
  }
}
