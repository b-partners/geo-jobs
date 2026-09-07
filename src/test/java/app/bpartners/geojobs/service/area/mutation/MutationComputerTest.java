package app.bpartners.geojobs.service.area.mutation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.rest.model.GeoServerParameter;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.ParcelContent;
import app.bpartners.geojobs.repository.model.detection.FeatureWithDelimitation;
import app.bpartners.geojobs.service.area.mutation.model.InstantTile;
import app.bpartners.geojobs.service.area.mutation.model.MutationContext;
import app.bpartners.geojobs.service.area.mutation.model.MutationResponse;
import app.bpartners.geojobs.service.area.mutation.model.MutationResponseStatus;
import app.bpartners.geojobs.service.area.mutation.model.MutationType;
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
  @SneakyThrows
  void apply_downloads_both_tile_images_and_delegates_to_the_mutation_api() {
    var oldFeature = Feature.builder().id("feature-old").build();
    var recentFeature = Feature.builder().id("feature-recent").build();
    var oldDate = Instant.parse("2023-06-01T00:00:00Z");
    var recentDate = Instant.parse("2024-06-01T00:00:00Z");
    var oldApiUrl = new URL("http://geoserver.test/old-wms");
    var recentApiUrl = new URL("http://geoserver.test/recent-wms");
    var oldGeoServerParameter = new GeoServerParameter();
    var recentGeoServerParameter = new GeoServerParameter();
    var older =
        new InstantTile(
            oldDate,
            List.of(new FeatureWithDelimitation(oldFeature, List.of())),
            new InstantTile.ImageSource(oldApiUrl, oldGeoServerParameter));
    var mostRecent =
        new InstantTile(
            recentDate,
            List.of(new FeatureWithDelimitation(recentFeature, List.of())),
            new InstantTile.ImageSource(recentApiUrl, recentGeoServerParameter));
    var maskImageFile = new File("mask.png");
    var context = new MutationContext(older, mostRecent, maskImageFile);
    var oldTileImageFile = new File("old-tile.png");
    var recentTileImageFile = new File("recent-tile.png");
    var expectedOldParcelContent =
        ParcelContent.builder()
            .id(oldFeature.getId())
            .feature(oldFeature)
            .geoServerUrl(oldApiUrl)
            .geoServerParameter(oldGeoServerParameter)
            .creationDatetime(oldDate)
            .build();
    var expectedRecentParcelContent =
        ParcelContent.builder()
            .id(recentFeature.getId())
            .feature(recentFeature)
            .geoServerUrl(recentApiUrl)
            .geoServerParameter(recentGeoServerParameter)
            .creationDatetime(recentDate)
            .build();
    when(tilesDownloaderMock.apply(expectedOldParcelContent)).thenReturn(oldTileImageFile);
    when(tilesDownloaderMock.apply(expectedRecentParcelContent)).thenReturn(recentTileImageFile);
    var mutationResponse =
        new MutationResponse(
            MutationResponseStatus.SUCCESS, MutationType.DETERIORATION, "some-filename");
    when(apiMock.detectMutation(
            eq(oldTileImageFile), eq(recentTileImageFile), eq(maskImageFile), any()))
        .thenReturn(mutationResponse);

    var actual = subject.apply(context);

    assertEquals(MutationType.DETERIORATION, actual);
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
