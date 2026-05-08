package app.bpartners.geojobs.service.cityjson.texture;

import app.bpartners.geojobs.service.cityjson.texture.model.CityJsonWithVertices;
import app.bpartners.geojobs.service.cityjson.texture.model.RasterInfo;
import app.bpartners.geojobs.service.cityjson.texture.model.TextureInfo;
import app.bpartners.geojobs.service.cityjson.texture.model.TexturedCityJson;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CityJsonTextureComputer {
  private final CityJsonIOService cityJsonIOService;
  private final CityJsonTextureDomainService cityJsonTextureDomainService;

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

  public File textureCityJson(
      File cityJsonFile, File imageFile, double originX, double originY, double pixelSizeMeters) {
    ObjectNode json = cityJsonIOService.loadCityJson(cityJsonFile);
    CityJsonWithVertices cityJsonWithVertices = cityJsonTextureDomainService.toCityJsonFile(json);

    BufferedImage image;
    try {
      image = ImageIO.read(imageFile);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read image", e);
    }

    if (image == null) {
      throw new IllegalStateException("Could not read image: " + imageFile.getAbsolutePath());
    }

    RasterInfo rasterInfo =
        new RasterInfo(
            originX,
            originY,
            pixelSizeMeters,
            -pixelSizeMeters,
            0.0,
            0.0,
            image.getWidth(),
            image.getHeight());

    TextureInfo textureInfo = new TextureInfo(rasterInfo, imageFile);

    TexturedCityJson texturedCityJson =
        cityJsonTextureDomainService.texture(cityJsonWithVertices, textureInfo);

    return cityJsonIOService.toFile(texturedCityJson);
  }
}
