package app.bpartners.geojobs.service.google.geocoding;

import static app.bpartners.geojobs.model.CustomObjectMapper.objectMapper;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.rest.model.Point;
import app.bpartners.geojobs.model.geometry.RoofDetails;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import app.bpartners.geojobs.service.google.geocoding.api.GeoJsonFeature;
import app.bpartners.geojobs.service.google.geocoding.api.GoogleGeocodingClientApi;
import app.bpartners.geojobs.service.gouv.fr.rnb.RnbBuildingFinder;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.n52.jackson.datatype.jts.JtsModule;

class GoogleBuildingFinderTest {
  private static final Map<String, Object> POLYGON =
      Map.of(
          "type",
          "Polygon",
          "coordinates",
          List.of(List.of(List.of(2, 48), List.of(3, 48), List.of(3, 49), List.of(2, 48))));
  private static final Map<String, Object> MULTI_POLYGON =
      Map.of(
          "type",
          "MultiPolygon",
          "coordinates",
          List.of(
              List.of(List.of(List.of(2, 48), List.of(3, 48), List.of(3, 49), List.of(2, 48)))));

  GoogleGeocodingClientApi geocodingClientApiMock = mock();
  RnbBuildingFinder rnbBuildingFinderMock = mock();
  GoogleBuildingFinder subject =
      new GoogleBuildingFinder(
          geocodingClientApiMock,
          rnbBuildingFinderMock,
          objectMapper().registerModule(new JtsModule()));

  private static Optional<GeoJsonFeature> feature(Map<String, Object> geometry) {
    return Optional.of(GeoJsonFeature.of(geometry, Map.of()));
  }

  @Test
  void building_by_coordinates_wraps_a_polygon_into_a_multi_polygon() {
    when(geocodingClientApiMock.findBuildingByLocation(48.85, 2.35)).thenReturn(feature(POLYGON));

    var actual =
        subject.getBuildingMultiPolygon(
            List.of(BigDecimal.valueOf(2.35), BigDecimal.valueOf(48.85)));

    assertEquals(1, actual.getNumGeometries());
    assertEquals(0.5, actual.getArea(), 0.0001);
  }

  @Test
  void building_by_point_uses_the_point_coordinates_as_longitude_then_latitude() {
    when(geocodingClientApiMock.findBuildingByLocation(48.85, 2.35))
        .thenReturn(feature(MULTI_POLYGON));
    var point =
        new Point()
            .type(Point.TypeEnum.POINT)
            .coordinates(List.of(BigDecimal.valueOf(2.35), BigDecimal.valueOf(48.85)));

    var actual = subject.getBuildingMultiPolygon(point);

    assertEquals(1, actual.getNumGeometries());
  }

  @Test
  void building_by_address_returns_the_multi_polygon_given_by_google() {
    when(geocodingClientApiMock.findBuildingByAddress("1 rue de Paris"))
        .thenReturn(feature(MULTI_POLYGON));

    var actual = subject.getBuildingMultiPolygon("1 rue de Paris");

    assertEquals(1, actual.getNumGeometries());
  }

  @Test
  void building_is_null_when_google_finds_nothing() {
    when(geocodingClientApiMock.findBuildingByAddress("nowhere")).thenReturn(Optional.empty());

    assertNull(subject.getBuildingMultiPolygon("nowhere"));
  }

  @Test
  void building_is_null_when_google_gives_a_geometry_that_is_not_a_polygon() {
    when(geocodingClientApiMock.findBuildingByAddress("a point"))
        .thenReturn(feature(Map.of("type", "Point", "coordinates", List.of(2, 48))));

    assertNull(subject.getBuildingMultiPolygon("a point"));
  }

  @Test
  void building_is_null_when_google_geometry_cannot_be_read() {
    when(geocodingClientApiMock.findBuildingByAddress("broken"))
        .thenReturn(feature(Map.of("type", "Unknown")));

    assertNull(subject.getBuildingMultiPolygon("broken"));
  }

  @Test
  void roof_polygons_are_retrieved_from_rnb() {
    var ring = List.of(List.of(BigDecimal.ONE, BigDecimal.TEN));
    var roofs = List.of(new RoofDetails(new GeometryConverter().apply(List.of()), List.of("a")));
    when(rnbBuildingFinderMock.retrieveRoofPolygonsFrom(ring)).thenReturn(roofs);

    var actual = subject.retrieveRoofPolygonsFrom(ring);

    assertSame(roofs, actual);
    verify(rnbBuildingFinderMock).retrieveRoofPolygonsFrom(ring);
  }
}
