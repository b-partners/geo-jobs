package app.bpartners.geojobs.endpoint.rest.controller;

import static app.bpartners.geojobs.endpoint.rest.model.ImageUsage.ROOF_DAMAGE_DETECTION;
import static app.bpartners.geojobs.endpoint.rest.model.ZoneTilingJob.ZoomLevelEnum.HOUSES_0;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.rest.controller.v1.ImageController;
import app.bpartners.geojobs.endpoint.rest.model.ImageDetails;
import app.bpartners.geojobs.endpoint.rest.model.ImageZoomLevel;
import app.bpartners.geojobs.endpoint.rest.model.ZoneTilingJob;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.service.dashboard.AreaPictureApi;
import app.bpartners.geojobs.service.dashboard.FileApi;
import app.bpartners.geojobs.service.dashboard.component.AreaPictureDetails;
import app.bpartners.geojobs.service.dashboard.component.AreaPictureMapLayer;
import app.bpartners.geojobs.service.dashboard.component.CrupdateAreaPictureDetails;
import app.bpartners.geojobs.service.dashboard.component.FileType;
import java.math.BigDecimal;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;

class ImageControllerTest {
  AreaPictureApi areaPictureApiMock = mock();
  FileApi fileApiMock = mock();
  final String adminApiKey = randomUUID().toString();

  ImageController subject = new ImageController(areaPictureApiMock, fileApiMock, adminApiKey);

  @BeforeEach
  void setUp() {
    var areaPictureDetailsMock = mock(AreaPictureDetails.class);
    var areaPictureMapLayerMock = mock(AreaPictureMapLayer.class);
    when(areaPictureMapLayerMock.precisionLevelInCm()).thenReturn(5);
    when(areaPictureDetailsMock.actualLayer()).thenReturn(areaPictureMapLayerMock);
    when(areaPictureApiMock.crupdateAreaPictureDetails(any(), any(), any()))
        .thenReturn(areaPictureDetailsMock);
    when(fileApiMock.downloadOrUploadFile(any(), any(), any())).thenReturn(new byte[0]);
  }

  @Test
  void throw_exception_when_both_address_and_lon_lat_provided() {
    var actualOne =
        assertThrows(
            BadRequestException.class,
            () ->
                subject.getImage(
                    randomUUID().toString(), null, null, null, BigDecimal.ZERO, null, null));
    var actualTwo =
        assertThrows(
            BadRequestException.class,
            () ->
                subject.getImage(
                    randomUUID().toString(), null, null, null, null, BigDecimal.ZERO, null));
    var actualThree =
        assertThrows(
            BadRequestException.class,
            () ->
                subject.getImage(
                    randomUUID().toString(),
                    null,
                    null,
                    null,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    null));

    var expectedExceptionMessage = "Provide either an address or coordinates, not both";
    assertEquals(expectedExceptionMessage, actualOne.getMessage());
    assertEquals(expectedExceptionMessage, actualTwo.getMessage());
    assertEquals(expectedExceptionMessage, actualThree.getMessage());
  }

  @Test
  void throw_exception_when_either_address_or_lon_lat_not_provided() {
    var actual =
        assertThrows(
            BadRequestException.class,
            () -> subject.getImage(null, null, null, null, null, null, null));

    assertEquals(
        "Either address or both longitude and latitude must be provided", actual.getMessage());
  }

  @Test
  void get_images_ok() {
    var address = "some address";

    var actual = subject.getImage(address, null, null, null, null, null, null);

    var stringCaptor = ArgumentCaptor.forClass(String.class);
    verify(fileApiMock, only())
        .downloadOrUploadFile(stringCaptor.capture(), any(FileType.class), eq(adminApiKey));
    var fileIdGenerated = stringCaptor.getValue();
    assertEquals(
        new ImageDetails().address(address).imageBase64("data:image/jpeg;base64,"), actual);
    assertNotNull(fileIdGenerated);
  }

  @Test
  void get_images_for_detection_usage_ko() {
    reset(areaPictureApiMock);
    var areaPictureDetailsMock = mock(AreaPictureDetails.class);
    var areaPictureMapLayerMock = mock(AreaPictureMapLayer.class);
    when(areaPictureMapLayerMock.precisionLevelInCm()).thenReturn(20);
    when(areaPictureDetailsMock.actualLayer()).thenReturn(areaPictureMapLayerMock);
    when(areaPictureApiMock.crupdateAreaPictureDetails(any(), any(), any()))
        .thenReturn(areaPictureDetailsMock);
    var address = "some random address " + randomUUID();

    var actualException =
        assertThrows(
            NotImplementedException.class,
            () -> subject.getImage(address, null, null, null, null, null, ROOF_DAMAGE_DETECTION));

    assertEquals(
        "Unavailable images for address : "
            + address
            + " and usage ROOF_DAMAGE_DETECTION as image precision level is 20 cm",
        actualException.getMessage());
  }

  @Test
  void capture_crupdate_area_picture_details_when_get_images_ok() {
    var address = "other address";
    var isExtended = true;
    int providedShiftNb = 1;

    assertDoesNotThrow(
        () -> subject.getImage(address, null, isExtended, providedShiftNb, null, null, null));

    var crupdateAreaPictureDetailsCaptor =
        ArgumentCaptor.forClass(CrupdateAreaPictureDetails.class);
    verify(areaPictureApiMock, only())
        .crupdateAreaPictureDetails(
            any(String.class), crupdateAreaPictureDetailsCaptor.capture(), eq(adminApiKey));
    var actual = crupdateAreaPictureDetailsCaptor.getValue();
    var expected = expectedCrupdateAreaPictureDetails(address, actual);
    assertEquals(expected, actual);
  }

  private @NotNull CrupdateAreaPictureDetails expectedCrupdateAreaPictureDetails(
      String address, CrupdateAreaPictureDetails crupdateAreaPictureDetails) {
    int shiftNb = 1;
    boolean isExtended = true;
    String prospectId = null;
    var zoomLevel = HOUSES_0;
    return new CrupdateAreaPictureDetails(
        address,
        shiftNb,
        isExtended,
        crupdateAreaPictureDetails.fileId(),
        crupdateAreaPictureDetails.filename(),
        prospectId,
        zoomLevel);
  }

  @Test
  void get_images_ko() {
    reset(areaPictureApiMock);
    when(areaPictureApiMock.crupdateAreaPictureDetails(any(), any(), any()))
        .thenThrow(
            new HttpServerErrorException(
                HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"));
    String notFoundAddress = "not found address";
    var expectedMessageException = "Unable to retrieve image of address : " + notFoundAddress;

    var actual =
        assertThrows(
            ApiException.class,
            () -> subject.getImage(notFoundAddress, null, null, null, null, null, null));

    assertEquals(expectedMessageException, actual.getMessage());
  }

  private AreaPictureDetails detailsWithPositionAndTile() {
    var layer = mock(AreaPictureMapLayer.class);
    when(layer.precisionLevelInCm()).thenReturn(5);
    return new AreaPictureDetails(
        "areaPictureId",
        layer,
        new app.bpartners.geojobs.service.dashboard.component.GeoPosition(48.85, 2.35),
        new app.bpartners.geojobs.service.dashboard.component.TileCoordinates(
            100, 200, new app.bpartners.geojobs.service.dashboard.component.Zoom("HOUSES_0", 20)));
  }

  @Test
  void get_image_by_coordinates_uses_them_as_address_and_geo_position() {
    reset(areaPictureApiMock);
    var details = detailsWithPositionAndTile();
    when(areaPictureApiMock.crupdateAreaPictureDetails(any(), any(), any())).thenReturn(details);
    var longitude = BigDecimal.valueOf(2.5);
    var latitude = BigDecimal.valueOf(48.5);

    var actual = subject.getImage(null, null, null, null, longitude, latitude, null);

    assertNull(actual.getAddress());
    assertEquals(latitude, actual.getCurrentGeoPosition().getLatitude());
    assertEquals(longitude, actual.getCurrentGeoPosition().getLongitude());
    assertEquals(100, actual.getMinTileCoordinates().getX());
    assertEquals(200, actual.getMinTileCoordinates().getY());
    assertEquals(20, actual.getMinTileCoordinates().getZ());
    var detailsCaptor = ArgumentCaptor.forClass(CrupdateAreaPictureDetails.class);
    verify(areaPictureApiMock).crupdateAreaPictureDetails(any(), detailsCaptor.capture(), any());
    assertEquals("48.5,2.5", detailsCaptor.getValue().address());
  }

  @Test
  void get_image_by_address_uses_the_geo_position_found_for_the_address() {
    reset(areaPictureApiMock);
    var details = detailsWithPositionAndTile();
    when(areaPictureApiMock.crupdateAreaPictureDetails(any(), any(), any())).thenReturn(details);

    var actual = subject.getImage("some address", null, null, null, null, null, null);

    assertEquals("some address", actual.getAddress());
    assertEquals(BigDecimal.valueOf(48.85), actual.getCurrentGeoPosition().getLatitude());
    assertEquals(BigDecimal.valueOf(2.35), actual.getCurrentGeoPosition().getLongitude());
  }

  @Test
  void get_image_maps_the_requested_zoom_levels_to_tiling_zoom_levels() {
    subject.getImage("address", ImageZoomLevel.BUILDING, null, null, null, null, null);
    subject.getImage("address", ImageZoomLevel.NEIGHBORHOOD, null, null, null, null, null);
    subject.getImage("address", ImageZoomLevel.DISTRICT, null, null, null, null, null);

    var detailsCaptor = ArgumentCaptor.forClass(CrupdateAreaPictureDetails.class);
    verify(areaPictureApiMock, times(3))
        .crupdateAreaPictureDetails(any(), detailsCaptor.capture(), any());
    assertEquals(
        List.of(
            ZoneTilingJob.ZoomLevelEnum.HOUSES_0,
            ZoneTilingJob.ZoomLevelEnum.BUILDING,
            ZoneTilingJob.ZoomLevelEnum.BUILDINGS),
        detailsCaptor.getAllValues().stream().map(CrupdateAreaPictureDetails::zoomLevel).toList());
  }

  @Test
  void get_image_reports_area_picture_details_failures() {
    reset(areaPictureApiMock);
    when(areaPictureApiMock.crupdateAreaPictureDetails(any(), any(), any()))
        .thenThrow(new IllegalStateException("geodata down"));

    var error =
        assertThrows(
            ApiException.class,
            () -> subject.getImage("some address", null, null, null, null, null, null));

    assertEquals("Unable to retrieve image of address : some address", error.getMessage());
  }

  @Test
  void get_image_reports_file_download_failures() {
    reset(fileApiMock);
    when(fileApiMock.downloadOrUploadFile(any(), any(), any()))
        .thenThrow(new IllegalStateException("file api down"));

    var error =
        assertThrows(
            ApiException.class,
            () -> subject.getImage("some address", null, null, null, null, null, null));

    assertEquals("Unable to retrieve image of address : some address", error.getMessage());
  }
}
