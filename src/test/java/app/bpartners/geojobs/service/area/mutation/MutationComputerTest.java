package app.bpartners.geojobs.service.area.mutation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.rest.model.GeoServerParameter;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.ParcelContent;
import app.bpartners.geojobs.repository.model.detection.FeatureWithDelimitation;
import app.bpartners.geojobs.service.area.mutation.model.InstantParcel;
import app.bpartners.geojobs.service.area.mutation.model.MutationContext;
import app.bpartners.geojobs.service.tiling.downloader.TilesDownloader;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.time.Instant;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

class MutationComputerTest {
  private final MutationApi apiMock = mock(MutationApi.class);
  private final TilesDownloader tilesDownloaderMock = mock(TilesDownloader.class);
  private final MutationComputer subject = new MutationComputer(apiMock, tilesDownloaderMock);

  @Test
  void apply_throws_not_implemented_because_millesime_grouping_is_missing() {
    var context = mock(MutationContext.class);
    when(context.parcelDelimitations()).thenReturn(List.of());

    assertThrows(NotImplementedException.class, () -> subject.apply(context));
  }

  @Test
  void getMostRecentInstantParcel_throws_not_implemented() {
    var cause =
        assertThrows(
            InvocationTargetException.class,
            () ->
                invokePrivate(
                    "getMostRecentInstantParcel", new Class<?>[] {List.class}, List.of()));

    assertInstanceOf(NotImplementedException.class, cause.getCause());
  }

  @Test
  void getPrecedentInstantParcel_throws_not_implemented() {
    var someParcel = new InstantParcel(Instant.now(), List.of());

    var cause =
        assertThrows(
            InvocationTargetException.class,
            () ->
                invokePrivate(
                    "getPrecedentInstantParcel", new Class<?>[] {InstantParcel.class}, someParcel));

    assertInstanceOf(NotImplementedException.class, cause.getCause());
  }

  @Test
  void parcelImageFile_downloads_single_tile_image_from_parcel_content() throws Exception {
    var feature = Feature.builder().id("feature-1").build();
    var featureWithDelimitation = new FeatureWithDelimitation(feature, List.of());
    var date = Instant.parse("2024-06-01T00:00:00Z");
    var parcel = new InstantParcel(date, List.of(featureWithDelimitation));
    var geoServerUrl = new URL("http://geoserver.test/wms");
    var geoServerParameter = new GeoServerParameter();
    var maskImageFile = new File("mask.png");
    var context =
        new MutationContext(
            List.of(featureWithDelimitation), maskImageFile, geoServerUrl, geoServerParameter);
    var downloadedTile = new File("tile.png");
    when(tilesDownloaderMock.apply(any(ParcelContent.class))).thenReturn(downloadedTile);

    var actual =
        (File)
            invokePrivate(
                "parcelImageFile",
                new Class<?>[] {MutationContext.class, InstantParcel.class},
                context,
                parcel);

    assertEquals(downloadedTile, actual);
    var expectedParcelContent =
        ParcelContent.builder()
            .id(feature.getId())
            .feature(feature)
            .geoServerUrl(geoServerUrl)
            .geoServerParameter(geoServerParameter)
            .creationDatetime(date)
            .build();
    verify(tilesDownloaderMock).apply(expectedParcelContent);
  }

  @Test
  void singleTileImage_returns_file_when_not_a_directory() {
    var fileMock = mock(File.class);
    when(fileMock.isDirectory()).thenReturn(false);

    var actual = invokePrivate("singleTileImage", new Class<?>[] {File.class}, fileMock);

    assertEquals(fileMock, actual);
  }

  @Test
  void singleTileImage_recurses_into_first_child_of_a_directory() {
    var childFileMock = mock(File.class);
    when(childFileMock.isDirectory()).thenReturn(false);
    var directoryMock = mock(File.class);
    when(directoryMock.isDirectory()).thenReturn(true);
    when(directoryMock.listFiles()).thenReturn(new File[] {childFileMock});

    var actual = invokePrivate("singleTileImage", new Class<?>[] {File.class}, directoryMock);

    assertEquals(childFileMock, actual);
  }

  @Test
  void singleTileImage_throws_when_directory_has_no_children() {
    var directoryMock = mock(File.class);
    when(directoryMock.isDirectory()).thenReturn(true);
    when(directoryMock.listFiles()).thenReturn(new File[0]);

    var cause =
        assertThrows(
            InvocationTargetException.class,
            () -> invokePrivate("singleTileImage", new Class<?>[] {File.class}, directoryMock));

    assertInstanceOf(IllegalStateException.class, cause.getCause());
  }

  @Test
  void singleTileImage_throws_when_listing_directory_children_fails() {
    var directoryMock = mock(File.class);
    when(directoryMock.isDirectory()).thenReturn(true);
    when(directoryMock.listFiles()).thenReturn(null);

    var cause =
        assertThrows(
            InvocationTargetException.class,
            () -> invokePrivate("singleTileImage", new Class<?>[] {File.class}, directoryMock));

    assertInstanceOf(IllegalStateException.class, cause.getCause());
  }

  @SneakyThrows
  private Object invokePrivate(String methodName, Class<?>[] paramTypes, Object... args) {
    Method method = MutationComputer.class.getDeclaredMethod(methodName, paramTypes);
    method.setAccessible(true);
    return method.invoke(subject, args);
  }
}
