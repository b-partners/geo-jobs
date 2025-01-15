package app.bpartners.geojobs.model.geometry.plot;

import static java.awt.Color.BLACK;
import static java.awt.Color.RED;

import app.bpartners.geojobs.model.geometry.IntXY;
import app.bpartners.geojobs.model.geometry.quadrilateral.model.Quadrilateral;
import java.awt.*;

public final class PlotableQuadrilateral extends Plotable {
  private final IntXY a;
  private final IntXY b;
  private final IntXY c;
  private final IntXY d;

  public PlotableQuadrilateral(Quadrilateral quadrilateral) {
    super(new PlotConf());
    this.a = new IntXY((int) quadrilateral.a().x, (int) quadrilateral.a().y);
    this.b = new IntXY((int) quadrilateral.b().x, (int) quadrilateral.b().y);
    this.c = new IntXY((int) quadrilateral.c().x, (int) quadrilateral.c().y);
    this.d = new IntXY((int) quadrilateral.d().x, (int) quadrilateral.d().y);
  }

  @Override
  public void draw(Graphics2D g2d) {
    if (plotConf.scale() != 1) {
      throw new RuntimeException("Drawing quadrilateral with scale!=1 is not supported yet");
    }

    g2d.setColor(RED);
    g2d.drawPolygon(
        new int[] {a.x(), b.x(), c.x(), d.x()}, new int[] {a.y(), b.y(), c.y(), d.y()}, 4);

    g2d.setColor(BLACK);
    g2d.drawChars("a".toCharArray(), 0, 1, a.x(), a.y());
    g2d.drawChars("b".toCharArray(), 0, 1, b.x(), b.y());
    g2d.drawChars("c".toCharArray(), 0, 1, c.x(), c.y());
    g2d.drawChars("d".toCharArray(), 0, 1, d.x(), d.y());
  }
}
