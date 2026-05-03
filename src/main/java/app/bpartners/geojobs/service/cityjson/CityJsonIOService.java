package app.bpartners.geojobs.service.cityjson;

import app.bpartners.geojobs.service.cityjson.model.RasterInfo;
import app.bpartners.geojobs.service.cityjson.model.TextureFile;
import app.bpartners.geojobs.service.cityjson.model.TexturedCityJson;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;

@Service
public class CityJsonIOService {

  private final ObjectMapper objectMapper = new ObjectMapper();

  public ObjectNode loadCityJson(Path path) throws IOException {
    return (ObjectNode) objectMapper.readTree(path.toFile());
  }

  public TextureFile loadTexture(Path path) throws IOException {
    RasterInfo rasterInfo = readRasterInfo(path);
    String dataUri = imageToDataUri(path);
    return new TextureFile(dataUri, rasterInfo);
  }

  public void save(TexturedCityJson texturedCityJson, Path outputPath) throws IOException {
    if (outputPath.getParent() != null) {
      Files.createDirectories(outputPath.getParent());
    }
    objectMapper
        .writerWithDefaultPrettyPrinter()
        .writeValue(outputPath.toFile(), texturedCityJson.json());
  }

  public String imageToDataUri(Path tifPath) throws IOException {
    BufferedImage image = ImageIO.read(tifPath.toFile());
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      ImageIO.write(image, "png", baos);
      byte[] imageBytes = baos.toByteArray();
      String base64Image = Base64.getEncoder().encodeToString(imageBytes);
      return "data:image/png;base64," + base64Image;
    }
  }

  public String saveTexture(Path tifPath, Path outputDirectory) throws IOException {
    Files.createDirectories(outputDirectory);

    BufferedImage image = ImageIO.read(tifPath.toFile());

    if (image == null) {
      throw new IOException("Could not read GeoTIFF image: " + tifPath);
    }

    BufferedImage rgbImage =
        new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);

    Graphics2D graphics = rgbImage.createGraphics();
    graphics.drawImage(image, 0, 0, null);
    graphics.dispose();

    Path outputPath = outputDirectory.resolve("texture.png");

    ImageIO.write(rgbImage, "png", outputPath.toFile());

    return outputPath.toAbsolutePath().toString();
  }

  public RasterInfo readRasterInfo(Path tifPath) throws IOException {
    BufferedImage image = ImageIO.read(tifPath.toFile());

    if (image == null) {
      throw new IOException("Could not read raster dimensions: " + tifPath);
    }

    double originX = 0.0;
    double originY = 0.0;
    double pixelWidth = 1.0;
    double pixelHeight = -1.0;

    try {
      Process process = new ProcessBuilder("gdalinfo", "-json", tifPath.toString()).start();
      JsonNode gdalJson = objectMapper.readTree(process.getInputStream());
      if (gdalJson.has("geoTransform")) {
        JsonNode gt = gdalJson.get("geoTransform");
        originX = gt.get(0).asDouble();
        pixelWidth = gt.get(1).asDouble();
        originY = gt.get(3).asDouble();
        pixelHeight = gt.get(5).asDouble();
      }
    } catch (Exception e) {
      System.err.println(
          "Warning: Could not read GeoTIFF transform using gdalinfo: " + e.getMessage());
    }

    return new RasterInfo(
        originX, originY, pixelWidth, pixelHeight, 0.0, 0.0, image.getWidth(), image.getHeight());
  }
}
