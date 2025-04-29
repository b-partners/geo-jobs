package app.bpartners.geojobs.service.dashboard.component;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AreaPictureMapLayer(String id, String name, Zoom maximumZoom) {

  public record Zoom(String level, int number) {}
}
