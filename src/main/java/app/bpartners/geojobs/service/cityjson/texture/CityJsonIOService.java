package app.bpartners.geojobs.service.cityjson.texture;

import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.service.cityjson.texture.model.TexturedCityJson;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;

@Service
public class CityJsonIOService {

  private final ObjectMapper objectMapper = new ObjectMapper();

  public ObjectNode loadCityJsonFile(File file) {
    try {
      return (ObjectNode) objectMapper.readTree(file);
    } catch (IOException e) {
      throw new IllegalStateException("Could not read CityJSON file", e);
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

  public String imageToDataUri(File file) {
    try {
      BufferedImage image = ImageIO.read(file);
      try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
        ImageIO.write(image, "png", baos);
        byte[] imageBytes = baos.toByteArray();
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        return "data:image/png;base64," + base64Image;
      } catch (IOException e) {
        throw new IllegalStateException("Could not write image", e);
      }
    } catch (Exception e) {
      throw new IllegalStateException("Could not read image", e);
    }
  }
}
