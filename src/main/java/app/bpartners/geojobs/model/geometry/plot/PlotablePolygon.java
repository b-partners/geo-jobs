package app.bpartners.geojobs.model.geometry.plot;

import static app.bpartners.geojobs.model.geometry.plot.PlotConf.DEFAULT_SCALE;
import static app.bpartners.geojobs.model.geometry.plot.PlotConf.DEFAULT_STROKE;

import java.awt.*;
import java.util.Arrays;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

public final class PlotablePolygon extends Plotable {

  private final Polygon polygon;

  public PlotablePolygon(Polygon polygon, PlotConf plotConf) {
    super(plotConf);
    this.polygon = polygon;
  }

  public PlotablePolygon(Polygon polygon, Color color) {
    this(polygon, new PlotConf(color, DEFAULT_STROKE, DEFAULT_SCALE));
  }

  @Override
  public void draw(Graphics2D g2d) {
    var scale = plotConf.scale();
    draw(g2d, polygon.getExteriorRing().getCoordinates(), scale);
    for (int n = 0; n < polygon.getNumInteriorRing(); n++) {
      draw(g2d, polygon.getInteriorRingN(n).getCoordinates(), scale);
    }
  }

  private static void draw(Graphics2D g2d, Coordinate[] coordinates, double scale) {
    g2d.drawPolygon(
        Arrays.stream(coordinates).mapToInt(c -> (int) (c.x * scale)).toArray(),
        Arrays.stream(coordinates).mapToInt(c -> (int) (c.y * scale)).toArray(),
        coordinates.length);
  }
}
