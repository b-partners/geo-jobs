package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.repository.model.ArcgisImageZoom.HOUSES_0;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.model.TileExtendedImageRequested;
import app.bpartners.geojobs.file.ExtensionGuesser;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.file.hash.FileHash;
import app.bpartners.geojobs.service.event.TileExtendedImageRequestedService;
import app.bpartners.geojobs.service.tile19.ExtenderApi;
import app.bpartners.geojobs.service.tiling.TileFinder;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.math.BigDecimal;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.ClassPathResource;

class TileExtendedImageRequestedServiceIT {
  BucketComponent bucketComponentMock = mock(BucketComponent.class);
  TileFinder tileFinder = new TileFinder();
  ExtenderApi extenderApi = new ExtenderApi();
  FileWriter fileWriter = new FileWriter(new ObjectMapper(), new ExtensionGuesser());

  TileExtendedImageRequestedService subject =
      new TileExtendedImageRequestedService(
          tileFinder, bucketComponentMock, extenderApi, fileWriter);

  @SneakyThrows
  @BeforeEach
  void setUp() {
    when(bucketComponentMock.download(any()))
        .thenReturn(new ClassPathResource("/images/extender/61-92.jpg").getFile())
        .thenReturn(new ClassPathResource("/images/extender/62-92.jpg").getFile())
        .thenReturn(new ClassPathResource("/images/extender/63-92.jpg").getFile())
        .thenReturn(new ClassPathResource("/images/extender/61-93.jpg").getFile())
        .thenReturn(new ClassPathResource("/images/extender/62-93.jpg").getFile())
        .thenReturn(new ClassPathResource("/images/extender/63-93.jpg").getFile())
        .thenReturn(new ClassPathResource("/images/extender/61-94.jpg").getFile())
        .thenReturn(new ClassPathResource("/images/extender/62-94.jpg").getFile())
        .thenReturn(new ClassPathResource("/images/extender/63-94.jpg").getFile());

    when(bucketComponentMock.upload(any(), any())).thenReturn(mock(FileHash.class));
  }

  @Test
  void extend_image() {
    var latitude = BigDecimal.valueOf(46.651930);
    var longitude = BigDecimal.valueOf(-0.249317);
    var layer = "cite:PCRS";
    var zoomLevel = HOUSES_0.getZoomLevel();

    assertDoesNotThrow(
        () ->
            subject.accept(new TileExtendedImageRequested(longitude, latitude, zoomLevel, layer)));

    var fileCaptor = ArgumentCaptor.forClass(File.class);
    var stringCaptor = ArgumentCaptor.forClass(String.class);
    verify(bucketComponentMock).upload(fileCaptor.capture(), stringCaptor.capture());
    var extendedFile = fileCaptor.getValue();
    var extendedFileKey = stringCaptor.getValue();
    var expectedKey =
        layer
            + "/extended_original_"
            + longitude.doubleValue()
            + "_"
            + latitude.doubleValue()
            + ".jpg";
    assertEquals(expectedKey, extendedFileKey);
    assertNotNull(extendedFile);
  }
}
