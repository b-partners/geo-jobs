package app.bpartners.geojobs.model.geometry.plot;

import static java.awt.Color.WHITE;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static java.nio.file.Files.createTempFile;
import static java.util.stream.Collectors.toSet;

import app.bpartners.geojobs.model.geometry.quadrilateral.model.Quadrilateral;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Set;
import javax.imageio.ImageIO;
import org.locationtech.jts.geom.Polygon;

public record PlotablePlane(int width, int height) {

  public BufferedImage plotQuadrilaterals(Set<Quadrilateral> quadrilaterals) {
    return plot(quadrilaterals.stream().map(PlotableQuadrilateral::new).collect(toSet()));
  }

  public BufferedImage plot(Set<Plotable> plotables, Stroke stroke, double scale) {
    var bufferedImage = new BufferedImage(width, height, TYPE_INT_ARGB);
    var g2d = bufferedImage.createGraphics();
    g2d.setColor(WHITE);
    g2d.fillRect(0, 0, width, height);

    if (stroke != null) {
      g2d.setStroke(stroke);
    }
    plotables.forEach(q -> q.plot(g2d, scale));

    g2d.dispose();

    try {
      var outputfile = createTempFile("plane", ".png").toFile();
      ImageIO.write(bufferedImage, "png", outputfile);
      System.out.println("Plane plotted in: " + outputfile);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return bufferedImage;
  }

  public BufferedImage plot(Set<Plotable> plotables) {
    return plot(plotables, null, 1);
  }

  public BufferedImage plot(Set<Polygon> polygons, Color color) {
    return plot(polygons, color, null, 1);
  }

  public BufferedImage plot(Set<Polygon> polygons, Color color, Stroke stroke, double scale) {
    return plot(
        polygons.stream().map(p -> new PlotablePolygon(p, color)).collect(toSet()), stroke, scale);
  }
}
