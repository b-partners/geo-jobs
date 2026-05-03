package app.bpartners.geojobs.service.cityjson;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CityJsonTextureServiceTest {

  CityJsonTextureService subject = new CityJsonTextureService();

  @Test
  void texturize() throws IOException {
    Path cityJsonPath = new ClassPathResource("cityjson/texture/inputs/roof5/roof5.json").getFile().toPath();
    Path tifPath = new ClassPathResource("cityjson/texture/inputs/roof5/roof5.tif").getFile().toPath();
    Path outputDirectory = Path.of("test_output");
    Files.createDirectories(outputDirectory);
    int roofNumber = 5;

    subject.textureCityJson(cityJsonPath, tifPath, outputDirectory, roofNumber);

    Path expectedOutputFile = outputDirectory.resolve("roof" + roofNumber + ".json");
    assertTrue(Files.exists(expectedOutputFile), "Output file should exist: " + expectedOutputFile);
    assertTrue(Files.size(expectedOutputFile) > 0, "Output file should not be empty");

    Path expectedTextureFile = outputDirectory.resolve("texture.png");
    assertTrue(Files.exists(expectedTextureFile), "Texture file should exist: " + expectedTextureFile);
    assertTrue(Files.size(expectedTextureFile) > 0, "Texture file should not be empty");
  }
}