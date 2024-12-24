package app.bpartners.geojobs.model.geometry.plot;

import java.awt.*;
import lombok.AllArgsConstructor;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

@AllArgsConstructor
public class PlotableMultiPolygon implements Plotable {
  private final MultiPolygon polygons;
  private final Color color;

  @Override
  public void plot(Graphics2D g2d) {
    for (int n = 0; n < polygons.getNumGeometries(); n++) {
      new PlotablePolygon((Polygon) polygons.getGeometryN(n), color).plot(g2d);
    }
  }
}
