package app.bpartners.geojobs.service.cityjson.texture;

import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.service.cityjson.texture.model.CityJsonWithVertices;
import app.bpartners.geojobs.service.cityjson.texture.model.RasterInfo;
import app.bpartners.geojobs.service.cityjson.texture.model.TextureInfo;
import app.bpartners.geojobs.service.cityjson.texture.model.TexturedCityJson;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CityJsonTextureComputer {
  private final CityJsonIOService cityJsonIOService;
  private final CityJsonTextureDomainService cityJsonTextureDomainService;

  public CityJsonTextureComputer(BucketComponent bucketComponent) {
    this.cityJsonIOService = new CityJsonIOService();
    this.cityJsonTextureDomainService = new CityJsonTextureDomainService(this.cityJsonIOService, bucketComponent);
  }

  public File textureCityJson(File cityJsonFile, RasterInfo rasterInfo, String imageDataUri) {
    ObjectNode json = cityJsonIOService.loadCityJson(cityJsonFile);
    CityJsonWithVertices cityJsonWithVertices = cityJsonTextureDomainService.toCityJsonFile(json);
    TextureInfo textureInfo = new TextureInfo(rasterInfo, null, imageDataUri);

    TexturedCityJson texturedCityJson =
        cityJsonTextureDomainService.texture(cityJsonWithVertices, textureInfo);

    return cityJsonIOService.toFile(texturedCityJson);
  }

  public File textureCityJson(File cityJsonFile, File tifFile) {
    ObjectNode json = cityJsonIOService.loadCityJson(cityJsonFile);
    CityJsonWithVertices cityJsonWithVertices = cityJsonTextureDomainService.toCityJsonFile(json);
    TextureInfo textureInfo = cityJsonIOService.loadTexture(tifFile);

    TexturedCityJson texturedCityJson =
        cityJsonTextureDomainService.texture(cityJsonWithVertices, textureInfo);

    return cityJsonIOService.toFile(texturedCityJson);
  }
}
