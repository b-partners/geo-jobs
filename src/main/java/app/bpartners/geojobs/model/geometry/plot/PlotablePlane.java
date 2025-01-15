package app.bpartners.geojobs.model.geometry.plot;

import app.bpartners.geojobs.model.geometry.quadrilateral.model.Quadrilateral;
import lombok.SneakyThrows;
import org.locationtech.jts.geom.Polygon;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Set;

import static app.bpartners.geojobs.model.geometry.plot.PlotConf.DEFAULT_STROKE;
import static java.awt.Color.WHITE;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static java.nio.file.Files.createTempFile;
import static java.util.stream.Collectors.toSet;

public record PlotablePlane(int width, int height) {

  public BufferedImage plotQuadrilaterals(Set<Quadrilateral> quadrilaterals) {
    return plot(quadrilaterals.stream().map(PlotableQuadrilateral::new).collect(toSet()));
  }

  @SneakyThrows
  public BufferedImage plot(Set<Plotable> plotables, PlotConf plotConf) {
    var bufferedImage = new BufferedImage(width, height, TYPE_INT_ARGB);
    var g2d = bufferedImage.createGraphics();
    g2d.setColor(WHITE);
    g2d.fillRect(0, 0, width, height);

    g2d.setStroke(plotConf.stroke());
    plotables.forEach(q -> q.plot(g2d, plotConf.scale()));

    g2d.dispose();

    var outputfile = createTempFile("plane", ".png").toFile();
    ImageIO.write(bufferedImage, "png", outputfile);
    System.out.println("Plane plotted in: " + outputfile);
    return bufferedImage;
  }

  public BufferedImage plot(Set<Plotable> plotables) {
    return plot(plotables, new PlotConf());
  }

  public BufferedImage plot(Set<Polygon> polygons, Color color) {
    return plotPolygons(polygons, new PlotConf(color, DEFAULT_STROKE, 1));
  }

  public BufferedImage plotPolygons(Set<Polygon> polygons, PlotConf plotConf) {
    return plot(
        polygons.stream().map(p -> new PlotablePolygon(p, plotConf.color())).collect(toSet()),
        plotConf);
  }
}
