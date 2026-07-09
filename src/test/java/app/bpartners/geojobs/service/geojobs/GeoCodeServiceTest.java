package app.bpartners.geojobs.service.geojobs;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.repository.GeoCodingJobRepository;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.service.BuildingFinder;
import app.bpartners.geojobs.service.GeoCodeService;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import app.bpartners.geojobs.service.google.maps.GeoCodeApi;
import app.bpartners.geojobs.service.google.maps.GeoPosition;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.MultiPolygon;
import org.mockito.ArgumentCaptor;

class GeoCodeServiceTest {
  GeoCodeApi geoCodeApiMock = mock();
  GeometryConverter geometryConverterMock = mock();
  GeoCodingJobRepository geoCodingJobRepositoryMock = mock();
  BucketComponent bucketComponentMock = mock();
  EventProducer eventProducerMock = mock();
  BuildingFinder buildingFinderMock = mock();

  GeoCodeService subject =
      new GeoCodeService(
          geoCodeApiMock,
          geometryConverterMock,
          geoCodingJobRepositoryMock,
          bucketComponentMock,
          eventProducerMock,
          buildingFinderMock);

  @Test
  void geocode_address_ok() {
    var address = "random-address-" + randomUUID();

    var mockFeature = mock(Feature.class);
    var multiPolygonMock = mock(MultiPolygon.class);
    when(buildingFinderMock.getBuildingMultiPolygon(address)).thenReturn(multiPolygonMock);
    when(geometryConverterMock.toFeature(eq(null), eq(20), any(), eq(multiPolygonMock)))
        .thenReturn(mockFeature);

    var actual = subject.geocode(address);

    assertEquals(mockFeature, actual);
  }

  @SneakyThrows
  @Test
  void geocode_address_through_point_ok() {
    var address = "random-address-" + randomUUID();
    var latitude = Math.random();
    var longitude = Math.random();

    var mockFeature = mock(Feature.class);
    var geoPositionMock = mock(GeoPosition.class);
    var multiPolygonMock = mock(MultiPolygon.class);
    when(geoPositionMock.latitude()).thenReturn(latitude);
    when(geoPositionMock.longitude()).thenReturn(longitude);
    when(geoCodeApiMock.searchGeoPositionFromAddress(address)).thenReturn(geoPositionMock);
    when(buildingFinderMock.getBuildingMultiPolygon(address))
        .thenThrow(new RuntimeException("Unexpected error"));
    when(buildingFinderMock.getBuildingMultiPolygon(
            List.of(BigDecimal.valueOf(longitude), BigDecimal.valueOf(latitude))))
        .thenReturn(multiPolygonMock);
    when(geometryConverterMock.toFeature(eq(null), eq(20), any(), eq(multiPolygonMock)))
        .thenReturn(mockFeature);

    var actual = subject.geocode(address);

    assertEquals(mockFeature, actual);
  }

  @Test
  void geocode_point_without_address_ok() {
    var latitude = BigDecimal.valueOf(48.85);
    var longitude = BigDecimal.valueOf(2.35);

    var mockFeature = mock(Feature.class);
    var multiPolygonMock = mock(MultiPolygon.class);
    when(buildingFinderMock.getBuildingMultiPolygon(List.of(longitude, latitude)))
        .thenReturn(multiPolygonMock);
    var propertiesCaptor = ArgumentCaptor.forClass(Map.class);
    when(geometryConverterMock.toFeature(
            eq(null), eq(20), propertiesCaptor.capture(), eq(multiPolygonMock)))
        .thenReturn(mockFeature);

    var actual = subject.geocode(null, longitude, latitude);

    assertEquals(mockFeature, actual);
    assertFalse(propertiesCaptor.getValue().containsKey("address"));
  }

  @Test
  void geocode_point_with_address_keeps_address_property_ok() {
    var address = "random-address-" + randomUUID();
    var latitude = BigDecimal.valueOf(48.85);
    var longitude = BigDecimal.valueOf(2.35);

    var mockFeature = mock(Feature.class);
    var multiPolygonMock = mock(MultiPolygon.class);
    when(buildingFinderMock.getBuildingMultiPolygon(List.of(longitude, latitude)))
        .thenReturn(multiPolygonMock);
    var propertiesCaptor = ArgumentCaptor.forClass(Map.class);
    when(geometryConverterMock.toFeature(
            eq(null), eq(20), propertiesCaptor.capture(), eq(multiPolygonMock)))
        .thenReturn(mockFeature);

    var actual = subject.geocode(address, longitude, latitude);

    assertEquals(mockFeature, actual);
    assertEquals(address, propertiesCaptor.getValue().get("address"));
  }

  @Test
  void geocode_address_missing_ko() {
    var actual = assertThrows(BadRequestException.class, () -> subject.geocode(null));
    var actualBlank = assertThrows(BadRequestException.class, () -> subject.geocode(""));

    assertEquals("Address is mandatory", actual.getMessage());
    assertEquals("Address is mandatory", actualBlank.getMessage());
  }

  @SneakyThrows
  @Test
  void geocode_address_ko() {
    var address = "random-address-" + randomUUID();
    when(buildingFinderMock.getBuildingMultiPolygon(address)).thenThrow(new RuntimeException());
    when(geoCodeApiMock.searchGeoPositionFromAddress(address)).thenThrow(new IOException());

    var actual = assertThrows(BadRequestException.class, () -> subject.geocode(address));

    assertEquals("Unable to geocode address : " + address, actual.getMessage());
  }
}
