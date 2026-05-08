package app.bpartners.geojobs.endpoint.rest.controller.mapper.cityjson;

import app.bpartners.geojobs.endpoint.rest.model.ThreeDTextureInfo;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONTexture;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static java.util.UUID.randomUUID;

@Component
@RequiredArgsConstructor
public class CityJSONTextureMapper {
  public CityJSONTexture toDomain(ThreeDTextureInfo texture) {
    return CityJSONTexture.builder()
        .id(randomUUID().toString())
        .imageUri(texture.getImageDataUri())
        .pixelWidth(texture.getPixelWidth())
        .pixelHeight(texture.getPixelHeight())
        .shearY(0.0)
        .shearX(0.0)
        .topLeftLongitude(texture.getTopLeftLon())
        .topLeftLatitude(texture.getTopLeftLat())
        .build();
  }
}
