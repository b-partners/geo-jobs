package app.bpartners.geojobs.service.coverage;

import app.bpartners.geojobs.endpoint.rest.model.ModelName;
import java.util.List;

public record Coverage2D(
    boolean covered, boolean hd, ImageryLayer layer, List<ModelName> supportedModels) {

  public static Coverage2D notCovered() {
    return new Coverage2D(false, false, null, List.of());
  }
}
