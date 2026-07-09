package app.bpartners.geojobs.endpoint.rest.controller;

import static app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.FeatureMapper.toRestFeature;
import static app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.MULTI_POLYGON;
import static app.bpartners.geojobs.model.CustomObjectMapper.objectMapper;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.rest.controller.v1.GeoCodeController;
import app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.GeoCodingJobRestMapper;
import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.endpoint.rest.security.AuthProvider;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.service.GeoCodeService;
import java.math.BigDecimal;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

class GeoCodeControllerTest {
  GeoCodeService serviceMock = mock();
  CommunityAuthorizationRepository communityAuthorizationRepositoryMock = mock();
  AuthProvider authProviderMock = mock();
  GeoCodingJobRestMapper geoCodingJobRestMapperMock = mock();

  GeoCodeController subject =
      new GeoCodeController(
          serviceMock,
          communityAuthorizationRepositoryMock,
          authProviderMock,
          geoCodingJobRestMapperMock);

  @SneakyThrows
  private static Feature domainFeature() {
    var coordinates =
        List.of(
            List.of(
                List.of(
                    List.of(new BigDecimal("2.35"), new BigDecimal("48.85")),
                    List.of(new BigDecimal("2.36"), new BigDecimal("48.85")),
                    List.of(new BigDecimal("2.36"), new BigDecimal("48.86")),
                    List.of(new BigDecimal("2.35"), new BigDecimal("48.85")))));
    return Feature.builder()
        .geometry(
            Feature.FeatureGeometry.builder()
                .geometryType(MULTI_POLYGON)
                .actualInstanceStringValue(
                    objectMapper().writeValueAsString(new MultiPolygon().coordinates(coordinates)))
                .build())
        .build();
  }

  @Test
  void get_geocode_by_address_ok() {
    var address = "1 rue de la vau saint jacques parthenay";
    var domainFeature = domainFeature();
    when(serviceMock.geocode(address)).thenReturn(domainFeature);

    var actual = subject.getGeocode(address, null, null);

    assertEquals(toRestFeature(domainFeature), actual);
    verify(serviceMock).geocode(address);
  }

  @Test
  void get_geocode_by_point_coordinates_ok() {
    var longitude = 2.35;
    var latitude = 48.85;
    var domainFeature = domainFeature();
    when(serviceMock.geocode(null, BigDecimal.valueOf(longitude), BigDecimal.valueOf(latitude)))
        .thenReturn(domainFeature);

    var actual = subject.getGeocode(null, latitude, longitude);

    assertEquals(toRestFeature(domainFeature), actual);
    verify(serviceMock).geocode(null, BigDecimal.valueOf(longitude), BigDecimal.valueOf(latitude));
  }

  @Test
  void get_geocode_by_point_coordinates_with_blank_address_ok() {
    var longitude = 2.35;
    var latitude = 48.85;
    var domainFeature = domainFeature();
    when(serviceMock.geocode(null, BigDecimal.valueOf(longitude), BigDecimal.valueOf(latitude)))
        .thenReturn(domainFeature);

    var actual = subject.getGeocode("  ", latitude, longitude);

    assertEquals(toRestFeature(domainFeature), actual);
    verify(serviceMock).geocode(null, BigDecimal.valueOf(longitude), BigDecimal.valueOf(latitude));
  }

  @Test
  void get_geocode_with_both_address_and_coordinates_ko() {
    var actualWithBoth =
        assertThrows(
            BadRequestException.class, () -> subject.getGeocode("some address", 48.85, 2.35));
    var actualWithLatitudeOnly =
        assertThrows(
            BadRequestException.class, () -> subject.getGeocode("some address", 48.85, null));
    var actualWithLongitudeOnly =
        assertThrows(
            BadRequestException.class, () -> subject.getGeocode("some address", null, 2.35));

    var expectedMessage =
        "Both address and point coordinates (longitude,latitude) can not be provided";
    assertEquals(expectedMessage, actualWithBoth.getMessage());
    assertEquals(expectedMessage, actualWithLatitudeOnly.getMessage());
    assertEquals(expectedMessage, actualWithLongitudeOnly.getMessage());
    verifyNoInteractions(serviceMock);
  }

  @Test
  void get_geocode_with_partial_coordinates_ko() {
    var actualWithLatitudeOnly =
        assertThrows(BadRequestException.class, () -> subject.getGeocode(null, 48.85, null));
    var actualWithLongitudeOnly =
        assertThrows(BadRequestException.class, () -> subject.getGeocode(null, null, 2.35));

    var expectedMessage =
        "Both longitude and latitude are required to geocode from point coordinates";
    assertEquals(expectedMessage, actualWithLatitudeOnly.getMessage());
    assertEquals(expectedMessage, actualWithLongitudeOnly.getMessage());
    verifyNoInteractions(serviceMock);
  }

  @Test
  void get_geocode_without_address_nor_coordinates_ko() {
    var actual =
        assertThrows(BadRequestException.class, () -> subject.getGeocode(null, null, null));
    var actualWithBlankAddress =
        assertThrows(BadRequestException.class, () -> subject.getGeocode("   ", null, null));

    var expectedMessage = "Either address or point coordinates (longitude,latitude) is required";
    assertEquals(expectedMessage, actual.getMessage());
    assertEquals(expectedMessage, actualWithBlankAddress.getMessage());
    verifyNoInteractions(serviceMock);
  }
}
