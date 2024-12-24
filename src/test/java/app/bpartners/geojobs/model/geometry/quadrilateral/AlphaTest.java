package app.bpartners.geojobs.model.geometry.quadrilateral;

import static app.bpartners.geojobs.model.geometry.TestData.compass1Polygon;
import static java.awt.Color.BLACK;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.model.geometry.plot.AreImagesEqual;
import app.bpartners.geojobs.model.geometry.plot.Plotable;
import app.bpartners.geojobs.model.geometry.plot.PlotablePlane;
import app.bpartners.geojobs.model.geometry.plot.PlotablePolygon;
import app.bpartners.geojobs.model.geometry.plot.PlotableQuadrilateral;
import app.bpartners.geojobs.model.geometry.quadrilateral.Alpha.AlphaConf;
import app.bpartners.geojobs.model.geometry.quadrilateral.model.OrientedQuadrilateral;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class AlphaTest {
  AreImagesEqual areImagesEqual = new AreImagesEqual(0.005); // note(numeric-instability)

  @Test
  void plot_alpha_longPolygon() throws IOException {
    var expected =
        ImageIO.read(this.getClass().getResourceAsStream("/geometry/compass1-alpha.png"));

    var polygon = compass1Polygon();
    Set<Plotable> plotables = new HashSet<>();
    plotables.add(new PlotablePolygon(polygon, BLACK));
    plotables.addAll(
        new Alpha(polygon, new AlphaConf(0.95, 100))
            .get().stream()
                .map(OrientedQuadrilateral::quadrilateral)
                .map(PlotableQuadrilateral::new)
                .collect(toSet()));
    var actual = new PlotablePlane(1024, 1024).plot(plotables);

    assertTrue(areImagesEqual.apply(expected, actual));
  }
}
