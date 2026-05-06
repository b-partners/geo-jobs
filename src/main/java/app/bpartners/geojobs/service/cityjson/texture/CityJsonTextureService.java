package app.bpartners.geojobs.service.cityjson.texture;

import app.bpartners.geojobs.service.cityjson.texture.model.*;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CityJsonTextureService {

  private final CityJsonIOService cityJsonIOService;
  private final CityJsonTextureDomainService cityJsonTextureDomainService;

  public File textureCityJson(File cityJsonFile, Texture texture) {
    ObjectNode json = cityJsonIOService.loadCityJsonFile(cityJsonFile);
    CityJsonWithVertices cityJsonWithVertices = cityJsonTextureDomainService.toCityJsonFile(json);

    TexturedCityJson texturedCityJson =
        cityJsonTextureDomainService.texture(cityJsonWithVertices, texture);

    return cityJsonIOService.toFile(texturedCityJson);
  }
}
