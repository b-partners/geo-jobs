package app.bpartners.geojobs.service.coverage;

public record Coverage2D(boolean covered, boolean hd, ImageryLayer layer) {

  public static Coverage2D notCovered() {
    return new Coverage2D(false, false, null);
  }
}
