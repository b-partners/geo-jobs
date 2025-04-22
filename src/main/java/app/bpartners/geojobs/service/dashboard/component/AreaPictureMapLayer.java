package app.bpartners.geojobs.service.dashboard.component;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Year;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AreaPictureMapLayer(
    String id, String name, Year year, String departementName, AreaPictureImageSource source) {
  public enum AreaPictureImageSource {
    OPENSTREETMAP,
    GEOSERVER,
    GEOSERVER_IGN
  }
}
