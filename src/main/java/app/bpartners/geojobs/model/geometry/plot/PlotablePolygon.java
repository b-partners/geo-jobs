package app.bpartners.geojobs.model.geometry.plot;

import java.awt.*;
import java.util.Arrays;
import lombok.AllArgsConstructor;
import org.locationtech.jts.geom.Polygon;

@AllArgsConstructor
public class PlotablePolygon implements Plotable {

  private final Polygon polygon;
  private final Color color;

  @Override
  public void plot(Graphics2D g2d, double scale) {
    g2d.setColor(color);
    var coordinates = polygon.getExteriorRing().getCoordinates();
    g2d.drawPolygon(
        Arrays.stream(coordinates).mapToInt(c -> (int) (c.x * scale)).toArray(),
        Arrays.stream(coordinates).mapToInt(c -> (int) (c.y * scale)).toArray(),
        coordinates.length);
  }
}
