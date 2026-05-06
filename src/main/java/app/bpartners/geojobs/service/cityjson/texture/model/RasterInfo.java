package app.bpartners.geojobs.service.cityjson.texture.model;

public record RasterInfo(double originX, double originY, double pixelWidth, double pixelHeight, double shearX,
                         double shearY, int width, int height) {
  private static final double DEFAULT_SHEAR_X = 0.0;
  private static final double DEFAULT_SHEAR_Y = 0.0;
  private static final double DEFAULT_PIXEL_WIDTH = 1.0;
  private static final double DEFAULT_PIXEL_HEIGHT = 1.0;

  public static RasterInfo of(double originX, double originY, int width, int height) {
    return new RasterInfo(originX, originY, DEFAULT_PIXEL_WIDTH, DEFAULT_PIXEL_HEIGHT, DEFAULT_SHEAR_X, DEFAULT_SHEAR_Y, width, height);
  }
}
