package app.bpartners.geojobs.service.cityjson.texture.model;

public record RasterInfo(
    double originX,
    double originY,
    double pixelWidth,
    double pixelHeight,
    int width,
    int height) {}
