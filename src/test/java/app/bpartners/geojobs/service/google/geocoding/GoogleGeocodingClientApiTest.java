package app.bpartners.geojobs.service.google.geocoding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import app.bpartners.geojobs.service.google.geocoding.api.GoogleGeocodingClientApi;
import app.bpartners.geojobs.service.google.geocoding.api.exception.GeocodingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class GoogleGeocodingClientApiTest {
  private static final String ENDPOINT = "https://geocode.googleapis.com/v4/geocode/destinations";
  private static final String API_KEY = "google-api-key";
  private static final String POLYGON =
      "{\"type\":\"Polygon\",\"coordinates\":[[[2,48],[2.1,48],[2.1,48.1],[2,48]]]}";

  RestTemplate restTemplate = new RestTemplate();
  MockRestServiceServer server;
  GoogleGeocodingClientApi subject = new GoogleGeocodingClientApi(restTemplate, API_KEY);

  @BeforeEach
  void setup() {
    server = MockRestServiceServer.bindTo(restTemplate).build();
  }

  private void respondWith(String body) {
    server
        .expect(requestTo(ENDPOINT))
        .andExpect(method(POST))
        .andExpect(header("X-Goog-Api-Key", API_KEY))
        .andExpect(header("X-Goog-FieldMask", "*"))
        .andRespond(withSuccess(body, APPLICATION_JSON));
  }

  @Test
  void building_by_address_returns_primary_polygon_with_metadata_and_address() {
    server
        .expect(requestTo(ENDPOINT))
        .andExpect(jsonPath("$.addressQuery.addressQuery").value("1 rue de Paris"))
        .andExpect(jsonPath("$.languageCode").value("fr"))
        .andExpect(jsonPath("$.regionCode").value("FR"))
        .andRespond(
            withSuccess(
                """
                {"destinations":[{"primary":{
                  "place":"places/abc","displayName":{"text":"Mairie","languageCode":"fr"},
                  "formattedAddress":"1 rue de Paris","structureType":"BUILDING",
                  "displayPolygon":%s}}]}
                """
                    .formatted(POLYGON),
                APPLICATION_JSON));

    var actual = subject.findBuildingByAddress("1 rue de Paris");

    assertTrue(actual.isPresent());
    var feature = actual.get();
    assertEquals("Feature", feature.type());
    assertEquals("Polygon", feature.geometry().get("type"));
    assertEquals("places/abc", feature.properties().get("placeId"));
    assertEquals("Mairie", feature.properties().get("displayName"));
    assertEquals("1 rue de Paris", feature.properties().get("formattedAddress"));
    assertEquals("BUILDING", feature.properties().get("structureType"));
    assertEquals("google-geocoding-v4", feature.properties().get("source"));
    assertEquals("1 rue de Paris", feature.properties().get("address"));
    server.verify();
  }

  @Test
  void building_by_location_queries_buildings_around_the_coordinates() {
    server
        .expect(requestTo(ENDPOINT))
        .andExpect(jsonPath("$.locationQuery.location.latitude").value(48.85))
        .andExpect(jsonPath("$.locationQuery.location.longitude").value(2.35))
        .andExpect(jsonPath("$.locationQuery.placeFilter.structureType").value("BUILDING"))
        .andRespond(
            withSuccess(
                """
                {"destinations":[{"primary":{"place":"places/xyz","displayPolygon":%s}}]}
                """
                    .formatted(POLYGON),
                APPLICATION_JSON));

    var actual = subject.findBuildingByLocation(48.85, 2.35);

    assertTrue(actual.isPresent());
    assertEquals("places/xyz", actual.get().properties().get("placeId"));
    assertTrue(!actual.get().properties().containsKey("displayName"));
    assertTrue(!actual.get().properties().containsKey("address"));
  }

  @Test
  void no_destination_gives_no_building() {
    respondWith("{\"destinations\":[]}");
    respondWith("{}");

    assertTrue(subject.findBuildingByAddress("nowhere").isEmpty());
    assertTrue(subject.findBuildingByAddress("nowhere").isEmpty());
  }

  @Test
  void address_without_primary_polygon_falls_back_on_first_containing_place() {
    respondWith(
        """
        {"destinations":[{"primary":{"place":"places/p"},
          "containingPlaces":[{"place":"places/containing","displayPolygon":%s}]}]}
        """
            .formatted(POLYGON));

    var actual = subject.findBuildingByAddress("1 rue de Paris");

    assertTrue(actual.isPresent());
    assertEquals("places/containing", actual.get().properties().get("placeId"));
  }

  @Test
  void address_without_any_polygon_gives_no_building() {
    respondWith("{\"destinations\":[{\"primary\":{\"place\":\"places/p\"}}]}");
    respondWith(
        "{\"destinations\":[{\"primary\":{\"place\":\"p\"},\"containingPlaces\":[{\"place\":\"c\"}]}]}");
    respondWith("{\"destinations\":[{\"primary\":{\"place\":\"p\"},\"containingPlaces\":[]}]}");

    assertTrue(subject.findBuildingByAddress("a").isEmpty());
    assertTrue(subject.findBuildingByAddress("b").isEmpty());
    assertTrue(subject.findBuildingByAddress("c").isEmpty());
  }

  @Test
  void location_without_primary_polygon_gives_no_building_without_fallback() {
    respondWith(
        """
        {"destinations":[{"primary":{"place":"places/p"},
          "containingPlaces":[{"place":"places/containing","displayPolygon":%s}]}]}
        """
            .formatted(POLYGON));

    var actual = subject.findBuildingByLocation(48.85, 2.35);

    assertTrue(actual.isEmpty());
  }

  @Test
  void api_error_status_is_reported_with_status_and_body() {
    server
        .expect(requestTo(ENDPOINT))
        .andRespond(withStatus(FORBIDDEN).body("quota exceeded").contentType(APPLICATION_JSON));

    var error =
        assertThrows(
            GeocodingException.class, () -> subject.findBuildingByAddress("1 rue de Paris"));

    assertTrue(error.getMessage().contains("403"));
    assertTrue(error.getMessage().contains("quota exceeded"));
  }

  @Test
  void unreadable_response_is_reported_as_a_call_error() {
    server.expect(requestTo(ENDPOINT)).andRespond(withSuccess("not json at all", APPLICATION_JSON));

    var error =
        assertThrows(
            GeocodingException.class, () -> subject.findBuildingByAddress("1 rue de Paris"));

    assertTrue(error.getMessage().startsWith("Erreur d'appel"));
  }
}
