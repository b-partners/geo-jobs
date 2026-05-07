package app.bpartners.geojobs.service.cityjson.io;

import static org.junit.jupiter.api.Assertions.*;

import app.bpartners.geojobs.service.cityjson.texture.CityJsonIOService;
import app.bpartners.geojobs.service.cityjson.texture.model.RasterInfo;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class CustomGeoTiffWriterTest {

  @Test
  void toGeoTiffFile_producesNonEmptyFile() throws IOException {
    CustomGeoTiffWriter writer = new CustomGeoTiffWriter();
    BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
    File result = writer.toGeoTiffFile(image, 0, 0, 0.1);

    assertNotNull(result);
    assertTrue(result.exists());
    assertTrue(
        result.length() > 0, "File should not be empty, but was " + result.length() + " bytes");

    CityJsonIOService ioService = new CityJsonIOService();
    RasterInfo info = ioService.readRasterInfo(result);

    assertEquals(0.0, info.originX(), 1e-6);
    assertEquals(0.0, info.originY(), 1e-6);
    assertEquals(0.1, info.pixelWidth(), 1e-6);
    assertEquals(-0.1, info.pixelHeight(), 1e-6);

    // Clean up
    result.delete();
  }
}
