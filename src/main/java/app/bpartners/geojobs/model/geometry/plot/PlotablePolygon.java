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
  public void plot(Graphics2D g2d) {
    g2d.setColor(color);
    var coordinates = polygon.getCoordinates();
    g2d.drawPolygon(
        Arrays.stream(coordinates).mapToInt(c -> (int) c.x).toArray(),
        Arrays.stream(coordinates).mapToInt(c -> (int) c.y).toArray(),
        coordinates.length);
  }
}
