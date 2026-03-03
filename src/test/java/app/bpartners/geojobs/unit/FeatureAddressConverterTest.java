package app.bpartners.geojobs.unit;

import static app.bpartners.geojobs.model.DelimitationObjectType.BUILDING;
import static app.bpartners.geojobs.model.DelimitationObjectType.PARCEL;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static software.amazon.awssdk.http.HttpStatusCode.*;

import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.service.FeatureAddressConverter;
import app.bpartners.geojobs.service.FeaturePointConverter;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import app.bpartners.geojobs.service.google.maps.GeoCodeApi;
import app.bpartners.geojobs.service.google.maps.GeoPosition;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.MultiPolygon;

class FeatureAddressConverterTest {
  GeoCodeApi geoCodeApiMock = mock();
  GeometryConverter geometryConverterMock = mock();
  FeaturePointConverter featurePointConverterMock =
      new FeaturePointConverter(geometryConverterMock);
  FeatureAddressConverter subject =
      new FeatureAddressConverter(geoCodeApiMock, featurePointConverterMock);

  @Test
  void throws_exception_when_delimitation_object_type_not_building() {
    var actual =
        assertThrows(
            NotImplementedException.class, () -> subject.apply(randomUUID().toString(), PARCEL));

    assertEquals(
        "Unable to convert address to Feature for delimitationObjectType PARCEL",
        actual.getMessage());
  }

  @SneakyThrows
  @Test
  void
      throws_exception_on_address_when_http_error_occurs_on_converting_address_to_point_geometry() {
    var randomAddress = "address " + randomUUID();
    when(geoCodeApiMock.searchGeoPositionFromAddress(randomAddress))
        .thenThrow(com.google.maps.errors.ApiException.class);

    var actualException =
        assertThrows(ApiException.class, () -> subject.apply(randomAddress, BUILDING));

    assertEquals(
        "Unable to convert address to GPS coordinate (longitude, latitude) : " + randomAddress,
        actualException.getMessage());
  }

  @SneakyThrows
  @Test
  void
      throws_exception_on_address_when_any_http_errors_occurs_on_converting_address_to_point_geometry_but_longitude_is_null() {
    var randomAddress = "address " + randomUUID();
    app.bpartners.geojobs.service.google.maps.GeoPosition geoPositionMock = mock();
    when(geoPositionMock.longitude()).thenReturn(null);
    when(geoCodeApiMock.searchGeoPositionFromAddress(randomAddress)).thenReturn(geoPositionMock);

    var actual = assertThrows(ApiException.class, () -> subject.apply(randomAddress, BUILDING));

    assertEquals(
        "Unable to convert address to GPS coordinate (longitude, latitude) : " + randomAddress,
        actual.getMessage());
  }

  @SneakyThrows
  @Test
  void return_converted_feature_when_address_is_converted_to_point_geometry() {
    var randomAddress = "address " + randomUUID();
    var longitudeDoubleValue = 1.0;
    var latitudeDoubleValue = 2.0;
    Feature convertedFeatureMock = mock();
    MultiPolygon nearestRoofMultiPolygonMock = mock();
    GeoPosition geoPositionMock = mock();
    when(geoPositionMock.longitude()).thenReturn(longitudeDoubleValue);
    when(geoPositionMock.latitude()).thenReturn(latitudeDoubleValue);
    when(geoCodeApiMock.searchGeoPositionFromAddress(randomAddress)).thenReturn(geoPositionMock);
    when(geometryConverterMock.retrieveNearestRoofMultiPolygon(
            List.of(
                BigDecimal.valueOf(longitudeDoubleValue), BigDecimal.valueOf(latitudeDoubleValue))))
        .thenReturn(nearestRoofMultiPolygonMock);
    when(geometryConverterMock.toFeature(
            anyString(), anyInt(), any(HashMap.class), any(MultiPolygon.class)))
        .thenReturn(convertedFeatureMock);

    assertDoesNotThrow(() -> subject.apply(randomAddress, BUILDING));

    HashMap<String, Object> expectedConvertedFeatureProperties = new HashMap<>();
    verify(geometryConverterMock, times(1))
        .toFeature(
            eq(null),
            eq(20),
            eq(expectedConvertedFeatureProperties),
            eq(nearestRoofMultiPolygonMock));
  }
}
