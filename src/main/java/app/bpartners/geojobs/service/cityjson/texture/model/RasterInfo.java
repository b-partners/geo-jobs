package app.bpartners.geojobs.service.cityjson.texture.model;

import app.bpartners.geojobs.repository.model.cityjson.CityJSONTexture;

public record RasterInfo(
    double originX,
    double originY,
    double pixelWidth,
    double pixelHeight,
    double shearX,
    double shearY,
    int width,
    int height) {
  public static RasterInfo of(CityJSONTexture texture) {
    return new RasterInfo(
        texture.getTopLeftLongitude(),
        texture.getTopLeftLatitude(),
        texture.getPixelWidth(),
        texture.getPixelHeight(),
        texture.getShearX(),
        texture.getShearY(),
        texture.getImageWidth(),
        texture.getImageHeight());
  }
}
