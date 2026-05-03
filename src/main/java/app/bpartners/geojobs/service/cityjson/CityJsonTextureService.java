package app.bpartners.geojobs.service.cityjson;

import app.bpartners.geojobs.service.cityjson.model.BuildingData;
import app.bpartners.geojobs.service.cityjson.model.CityJsonFile;
import app.bpartners.geojobs.service.cityjson.model.RasterInfo;
import app.bpartners.geojobs.service.cityjson.model.TextureFile;
import app.bpartners.geojobs.service.cityjson.model.TexturedBuildingData;
import app.bpartners.geojobs.service.cityjson.model.TexturedCityJson;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.file.Path;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CityJsonTextureService {

  private final CityJsonIOService cityJsonIOService;
  private final CityJsonTextureDomainService cityJsonTextureDomainService;

  public TexturedBuildingData texture(BuildingData buildingData, Path tifPath) throws IOException {
    RasterInfo rasterInfo = cityJsonIOService.readRasterInfo(tifPath);
    String textureDataUri = cityJsonIOService.imageToDataUri(tifPath);

    return cityJsonTextureDomainService.texture(buildingData, rasterInfo, textureDataUri);
  }

  public void textureCityJson(Path cityJsonPath, Path tifPath, Path outputDirectory, int roofNumber)
      throws IOException {
    Path outputPath = outputDirectory.resolve("roof" + roofNumber + ".json");

    ObjectNode json = cityJsonIOService.loadCityJson(cityJsonPath);
    CityJsonFile cityJsonFile = cityJsonTextureDomainService.toCityJsonFile(json);
    TextureFile textureFile = cityJsonIOService.loadTexture(tifPath);

    TexturedCityJson texturedCityJson =
        cityJsonTextureDomainService.texture(cityJsonFile, textureFile);

    cityJsonIOService.save(texturedCityJson, outputPath);
    cityJsonIOService.saveTexture(tifPath, outputDirectory);
  }

  public void buildTexturedCityJson(Path cityJsonPath, Path tifPath, Path outputPath)
      throws IOException {
    ObjectNode json = cityJsonIOService.loadCityJson(cityJsonPath);
    CityJsonFile cityJsonFile = cityJsonTextureDomainService.toCityJsonFile(json);
    TextureFile textureFile = cityJsonIOService.loadTexture(tifPath);

    TexturedCityJson texturedCityJson =
        cityJsonTextureDomainService.texture(cityJsonFile, textureFile);

    cityJsonIOService.save(texturedCityJson, outputPath);
  }
}
