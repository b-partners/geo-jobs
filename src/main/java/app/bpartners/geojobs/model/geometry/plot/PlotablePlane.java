package app.bpartners.geojobs.model.geometry.plot;

import static java.awt.Color.WHITE;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static java.nio.file.Files.createTempFile;
import static java.util.stream.Collectors.toSet;

import app.bpartners.geojobs.model.geometry.quadrilateral.model.Quadrilateral;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Set;
import javax.imageio.ImageIO;
import lombok.SneakyThrows;

public record PlotablePlane(int width, int height) {

  public BufferedImage plotQuadrilaterals(Set<Quadrilateral> quadrilaterals) {
    return plot(quadrilaterals.stream().map(PlotableQuadrilateral::new).collect(toSet()));
  }

  @SneakyThrows
  public BufferedImage plot(Set<Plotable> plotables) {
    var bufferedImage = new BufferedImage(width, height, TYPE_INT_ARGB);
    var g2d = bufferedImage.createGraphics();
    g2d.setColor(WHITE);
    g2d.fillRect(0, 0, width, height);

    plotables.forEach(q -> q.plot(g2d));

    g2d.dispose();

    String currentDir = "/Users/numer-mobile-2/geo-jobs/src/test/resources/geometry/output";

    File outputfile = new File(currentDir, "plane_" + System.nanoTime() + ".png");

    ImageIO.write(bufferedImage, "png", outputfile);
    System.out.println("Plane plotted in: " + outputfile);
    return bufferedImage;
  }
}
