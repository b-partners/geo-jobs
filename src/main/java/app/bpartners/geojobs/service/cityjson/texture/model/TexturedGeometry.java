package app.bpartners.geojobs.service.cityjson.texture.model;

import java.util.List;

public record TexturedGeometry(
    org.locationtech.jts.geom.Geometry geometry,
    java.util.Map<String, Object> properties,
    List<UV> uvs) {
  public record UV(double u, double v) {}
}
