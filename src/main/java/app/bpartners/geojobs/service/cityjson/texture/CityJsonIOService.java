package app.bpartners.geojobs.service.cityjson.texture;

import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.service.cityjson.texture.model.RasterInfo;
import app.bpartners.geojobs.service.cityjson.texture.model.TextureFile;
import app.bpartners.geojobs.service.cityjson.texture.model.TexturedCityJson;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import javax.imageio.ImageIO;

import lombok.extern.slf4j.Slf4j;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.geotools.api.referencing.datum.PixelInCell;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CityJsonIOService {

  private final ObjectMapper objectMapper = new ObjectMapper();

  public ObjectNode loadCityJson(File file) {
    try {
      return (ObjectNode) objectMapper.readTree(file);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read CityJSON file", e);
    }
  }

  public TextureFile loadTexture(File tifFile) {
    RasterInfo rasterInfo = readRasterInfo(tifFile);
    String dataUri = tifFile.getAbsolutePath(); // TODO: change into S3 URL
    return new TextureFile(dataUri, rasterInfo, tifFile);
  }

  public File toFile(TexturedCityJson texturedCityJson) {
    try {
      File file = File.createTempFile("textured-cityjson-" + randomUUID(), ".json");

      try (FileWriter writer = new FileWriter(file)) {
        writer.write(texturedCityJson.json().toString());
      }

      return file;

    } catch (IOException e) {
      throw new RuntimeException("Failed to write JSON to file", e);
    }
  }

  public void save(TexturedCityJson texturedCityJson, Path outputPath) throws IOException {
    if (outputPath.getParent() != null) {
      Files.createDirectories(outputPath.getParent());
    }
    objectMapper
        .writerWithDefaultPrettyPrinter()
        .writeValue(outputPath.toFile(), texturedCityJson.json());
  }

  public String imageToDataUri(File tifFile) {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      BufferedImage image = ImageIO.read(tifFile);
      ImageIO.write(image, "png", baos);
      byte[] imageBytes = baos.toByteArray();
      String base64Image = Base64.getEncoder().encodeToString(imageBytes);
      return "data:image/png;base64," + base64Image;
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read GeoTIFF image", e);
    }
  }

  public File saveTexture(File tifFile) {
    try {
      BufferedImage image = ImageIO.read(tifFile);

      if (image == null) {
        throw new IllegalStateException("Could not read GeoTIFF image: " + tifFile);
      }

      BufferedImage rgbImage =
          new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);

      Graphics2D graphics = rgbImage.createGraphics();
      graphics.drawImage(image, 0, 0, null);
      graphics.dispose();

      File pngFile = Files.createTempFile("texture-", ".png").toFile();
      ImageIO.write(rgbImage, "png", pngFile);

      return pngFile;
    } catch (IOException e) {
      throw new IllegalStateException("Failed to save GeoTIFF image as PNG image", e);
    }
  }

  public RasterInfo readRasterInfo(File tifFile) {
    BufferedImage image;
    try {
      image = ImageIO.read(tifFile);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read GeoTIFF image", e);
    }

    if (image == null) {
      throw new IllegalStateException(
          "Could not read raster dimensions: " + tifFile.getAbsolutePath());
    }

    double originX = 0.0;
    double originY = 0.0;
    double pixelWidth = 1.0;
    double pixelHeight = -1.0;

    GeoTiffReader reader = null;
    try {
      reader = new GeoTiffReader(tifFile);
      GridCoverage2D coverage = reader.read(null);
      MathTransform transform = coverage.getGridGeometry().getGridToCRS(PixelInCell.CELL_CORNER);
      if (transform instanceof AffineTransform affine) {
        originX = affine.getTranslateX();
        originY = affine.getTranslateY();
        pixelWidth = affine.getScaleX();
        pixelHeight = affine.getScaleY();
      }
    } catch (Exception e) {
      // Ignore and fallback to gdalinfo
    } finally {
      if (reader != null) {
        reader.dispose();
      }
    }

    if (originX == 0.0 && originY == 0.0 && pixelWidth == 1.0) {
      try {
        gdal.AllRegister();

        Dataset dataset = gdal.Open(tifFile.toString());

        if (dataset != null) {
          double[] geoTransform = dataset.GetGeoTransform();

          if (geoTransform != null && geoTransform.length >= 6) {
            originX = geoTransform[0];
            pixelWidth = geoTransform[1];
            originY = geoTransform[3];
            pixelHeight = geoTransform[5];
          }

          dataset.delete();
        }

      } catch (Exception e) {
        log.error("Warning: Could not read GeoTIFF transform using GDAL Java bindings: "
            + e.getMessage());
      }
    }

    return new RasterInfo(
        originX, originY, pixelWidth, pixelHeight, 0.0, 0.0, image.getWidth(), image.getHeight());
  }
}
