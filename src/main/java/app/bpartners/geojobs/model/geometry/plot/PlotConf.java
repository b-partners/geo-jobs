package app.bpartners.geojobs.model.geometry.plot;

import java.awt.*;

import static java.awt.Color.BLACK;

public record PlotConf(Color color, Stroke stroke, double scale) {
  public static final Color DEFAULT_COLOR = BLACK;
  public static final Stroke DEFAULT_STROKE = new BasicStroke(1);
  public static final int DEFAULT_SCALE = 1;

  public PlotConf() {
    this(DEFAULT_COLOR, DEFAULT_STROKE, DEFAULT_SCALE);
  }
}
