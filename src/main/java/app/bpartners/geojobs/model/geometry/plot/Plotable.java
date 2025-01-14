package app.bpartners.geojobs.model.geometry.plot;

import java.awt.*;

public interface Plotable {
  default void plot(Graphics2D g2d) {
    plot(g2d, 1);
  }

  void plot(Graphics2D g2d, double scale);
}
