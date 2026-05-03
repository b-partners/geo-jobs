package app.bpartners.geojobs.service.cityjson.model;

public record RasterInfo(
    double originX,
    double originY,
    double pixelWidth,
    double pixelHeight,
    double shearX,
    double shearY,
    int width,
    int height) {}
