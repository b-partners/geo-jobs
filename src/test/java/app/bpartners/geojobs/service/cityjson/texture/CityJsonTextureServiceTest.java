package app.bpartners.geojobs.service.cityjson.texture;

import static java.lang.Math.abs;
import static org.junit.jupiter.api.Assertions.*;

import app.bpartners.geojobs.service.cityjson.io.CustomGeoTiffWriter;
import app.bpartners.geojobs.service.cityjson.texture.model.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class CityJsonTextureServiceTest {

  CityJsonIOService cityJsonIOService = new CityJsonIOService();
  CityJsonTextureDomainService cityJsonTextureDomainService =
      new CityJsonTextureDomainService(cityJsonIOService);
  CityJsonTextureService subject =
      new CityJsonTextureService(cityJsonIOService, cityJsonTextureDomainService);

  @Test
  void texturize() throws IOException {
    int roofNumber = 4;
    File cityJsonFile =
        new ClassPathResource(
                String.format("cityjson/texture/inputs/roof%s/roof%s.json", roofNumber, roofNumber))
            .getFile();
    File tifFile =
        new ClassPathResource(
                String.format("cityjson/texture/inputs/roof%s/roof%s.tif", roofNumber, roofNumber))
            .getFile();

    subject.textureCityJson(cityJsonFile, tifFile);
  }

  @Test
  void texturize_from_geotiff_writer() throws IOException {
    CustomGeoTiffWriter geoTiffWriter = new CustomGeoTiffWriter();
    int roofNumber = 5;
    File cityJsonFile =
        new ClassPathResource(
                String.format("cityjson/texture/inputs/roof%s/roof%s.json", roofNumber, roofNumber))
            .getFile();
    File tifFile =
        new ClassPathResource(
                String.format("cityjson/texture/inputs/roof%s/roof%s.tif", roofNumber, roofNumber))
            .getFile();
    TextureFile textureFile = cityJsonIOService.loadTexture(tifFile);
    double pixelWidth = textureFile.rasterInfo().pixelWidth();
    double pixelHeight = textureFile.rasterInfo().pixelHeight();
    int pixelSize = Double.valueOf(((abs(pixelWidth) + abs(pixelHeight)) / 2) * 100).intValue();
    double originX = textureFile.rasterInfo().originX();
    double originY = textureFile.rasterInfo().originY();

    File imageFile =
        new ClassPathResource(
                String.format("cityjson/texture/outputs/roof%s/texture.png", roofNumber))
            .getFile();
    BufferedImage image = ImageIO.read(imageFile);
    File computedGeoTiffFile = geoTiffWriter.toGeoTiffFile(image, originX, originY, pixelSize);

    subject.textureCityJson(cityJsonFile, computedGeoTiffFile);
  }

  @Test
  void verifyEdgeMapping() {
    RasterInfo rasterInfo =
        new RasterInfo(
            100.0,
            200.0, // origin
            0.5,
            -0.5, // pixel size
            0.0,
            0.0, // shear
            100,
            100 // width, height (so 50x50 area)
            );

    // Top-left corner (origin)
    List<org.locationtech.jts.math.Vector3D> topLeft =
        List.of(new org.locationtech.jts.math.Vector3D(100.0, 200.0, 0));
    List<UV> uvTopLeft = cityJsonTextureDomainService.computeUv(topLeft, rasterInfo);
    assertEquals(0.0, uvTopLeft.get(0).u(), 1e-6, "Top-left U should be 0");
    assertEquals(1.0, uvTopLeft.get(0).v(), 1e-6, "Top-left V should be 1");

    // Bottom-right corner
    // x = 100 + 100 * 0.5 = 150
    // y = 200 + 100 * -0.5 = 150
    List<org.locationtech.jts.math.Vector3D> bottomRight =
        List.of(new org.locationtech.jts.math.Vector3D(150.0, 150.0, 0));
    List<UV> uvBottomRight = cityJsonTextureDomainService.computeUv(bottomRight, rasterInfo);
    assertEquals(1.0, uvBottomRight.get(0).u(), 1e-6, "Bottom-right U should be 1");
    assertEquals(0.0, uvBottomRight.get(0).v(), 1e-6, "Bottom-right V should be 0");

    // A point in the middle
    // x = 125, y = 175
    List<org.locationtech.jts.math.Vector3D> middle =
        List.of(new org.locationtech.jts.math.Vector3D(125.0, 175.0, 0));
    List<UV> uvMiddle = cityJsonTextureDomainService.computeUv(middle, rasterInfo);
    assertEquals(0.5, uvMiddle.get(0).u(), 1e-6, "Middle U should be 0.5");
    assertEquals(0.5, uvMiddle.get(0).v(), 1e-6, "Middle V should be 0.5");

    // Sub-pixel point
    // x = 100 + 0.5 * 0.5 = 100.25
    // y = 200 + 0.5 * -0.5 = 199.75
    List<org.locationtech.jts.math.Vector3D> subPixel =
        List.of(new org.locationtech.jts.math.Vector3D(100.125, 199.875, 0));
    List<UV> uvSubPixel = cityJsonTextureDomainService.computeUv(subPixel, rasterInfo);
    // col = (100.125 - 100) / 0.5 = 0.25
    // row = (199.875 - 200) / -0.5 = 0.25
    // u = 0.25 / 100 = 0.0025
    // v = 1.0 - (0.25 / 100) = 0.9975
    assertEquals(0.0025, uvSubPixel.get(0).u(), 1e-6, "Sub-pixel U should be 0.0025");
    assertEquals(0.9975, uvSubPixel.get(0).v(), 1e-6, "Sub-pixel V should be 0.9975");
  }
}
