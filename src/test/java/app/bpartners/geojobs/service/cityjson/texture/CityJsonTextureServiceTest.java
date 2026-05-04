package app.bpartners.geojobs.service.cityjson.texture;

import static org.junit.jupiter.api.Assertions.*;

import app.bpartners.geojobs.service.cityjson.texture.model.BuildingData;
import app.bpartners.geojobs.service.cityjson.texture.model.RasterInfo;
import app.bpartners.geojobs.service.cityjson.texture.model.TexturedBuildingData;
import app.bpartners.geojobs.service.cityjson.texture.model.UV;
import app.bpartners.geojobs.service.lidar.model.geometry.GeometryWithProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.springframework.core.io.ClassPathResource;

class CityJsonTextureServiceTest {

  CityJsonIOService cityJsonIOService = new CityJsonIOService();
  CityJsonTextureDomainService cityJsonTextureDomainService = new CityJsonTextureDomainService();
  CityJsonTextureService subject =
      new CityJsonTextureService(cityJsonIOService, cityJsonTextureDomainService);

  @Test
  void texturize() throws IOException {
    int roofNumber = 4;
    Path cityJsonPath =
        new ClassPathResource(
                String.format("cityjson/texture/inputs/roof%s/roof%s.json", roofNumber, roofNumber))
            .getFile()
            .toPath();
    Path tifPath =
        new ClassPathResource(
                String.format("cityjson/texture/inputs/roof%s/roof%s.tif", roofNumber, roofNumber))
            .getFile()
            .toPath();
    Path outputDirectory =
        Files.createTempDirectory(String.format("cityjson-texture-roof%s-", roofNumber));

    subject.textureCityJson(cityJsonPath, tifPath, outputDirectory, roofNumber);

    Path expectedOutputFile = outputDirectory.resolve("roof" + roofNumber + ".json");
    assertTrue(Files.exists(expectedOutputFile), "Output file should exist: " + expectedOutputFile);
    assertTrue(Files.size(expectedOutputFile) > 0, "Output file should not be empty");

    Path expectedTextureFile = outputDirectory.resolve("texture.png");
    assertTrue(
        Files.exists(expectedTextureFile), "Texture file should exist: " + expectedTextureFile);
    assertTrue(Files.size(expectedTextureFile) > 0, "Texture file should not be empty");
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

  @Test
  void testTextureBuildingData() throws IOException {
    Path tifPath =
        new ClassPathResource("cityjson/texture/inputs/roof4/roof4.tif").getFile().toPath();

    GeometryFactory gf = new GeometryFactory();
    Polygon polygon =
        gf.createPolygon(
            new Coordinate[] {
              new Coordinate(100.0, 200.0, 10.0),
              new Coordinate(150.0, 200.0, 10.0),
              new Coordinate(150.0, 150.0, 10.0),
              new Coordinate(100.0, 150.0, 10.0),
              new Coordinate(100.0, 200.0, 10.0)
            });

    BuildingData buildingData =
        BuildingData.builder()
            .id("test-building")
            .roofs(
                List.of(
                    GeometryWithProperties.builder()
                        .geometry(polygon)
                        .properties(Collections.emptyMap())
                        .build()))
            .walls(Collections.emptyList())
            .grounds(Collections.emptyList())
            .properties(Collections.emptyMap())
            .build();

    TexturedBuildingData textured = subject.texture(buildingData, tifPath);

    assertNotNull(textured);
    assertEquals("test-building", textured.id());
    assertEquals(1, textured.roofs().size());
    assertNotNull(textured.textureDataUri());
    assertTrue(textured.textureDataUri().startsWith("data:image/png;base64,"));

    var texturedRoof = textured.roofs().get(0);
    assertEquals(5, texturedRoof.uvs().size());
  }
}
