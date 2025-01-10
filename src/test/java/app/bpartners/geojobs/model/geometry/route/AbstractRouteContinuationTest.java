package app.bpartners.geojobs.model.geometry.route;

import static app.bpartners.geojobs.model.geometry.TestData.compass2Polygon;
import static app.bpartners.geojobs.model.geometry.TestData.long2Polygon;
import static java.awt.Color.BLACK;
import static java.lang.Math.PI;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.model.geometry.plot.AreImagesEqual;
import app.bpartners.geojobs.model.geometry.plot.PlotablePlane;
import app.bpartners.geojobs.model.geometry.plot.PlotablePolygon;
import app.bpartners.geojobs.model.geometry.quadrilateral.Alpha.AlphaConf;
import java.io.IOException;
import java.util.Set;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class AbstractRouteContinuationTest {

  AreImagesEqual areImagesEqual = new AreImagesEqual(0.0005); // note(numeric-instability)

  @Test
  void long2_compass2_continued() throws IOException {
    var expected =
        ImageIO.read(this.getClass().getResourceAsStream("/geometry/long2-compass2-continued.png"));
    var directionThreshold = PI / 4;
    var distanceThreshold = 1000;
    var alphaConf = new AlphaConf(0.95, 100);

    var long2 = long2Polygon();
    var compass2 = compass2Polygon();
    var lineContinuation =
        new AbstractRouteContinuation(
            new AbstractRoute(compass2, alphaConf),
            new AbstractRoute(long2, alphaConf),
            new ContinuationConf(directionThreshold, distanceThreshold));
    var actual =
        new PlotablePlane(1024, 1024)
            .plot(Set.of(new PlotablePolygon(lineContinuation.get().get(), BLACK)));

    assertTrue(areImagesEqual.apply(expected, actual));
  }

  @Test
  void long2_compass2_not_continued() throws IOException {
    var directionThreshold = PI / 6; // !!!!! Tolerance is too small: direction condition will fail
    var distanceThreshold = 1000;
    var alphaConf = new AlphaConf(0.95, 100);

    var long2 = long2Polygon();
    var compass2 = compass2Polygon();
    var lineContinuation =
        new AbstractRouteContinuation(
            new AbstractRoute(compass2, alphaConf),
            new AbstractRoute(long2, alphaConf),
            new ContinuationConf(directionThreshold, distanceThreshold));
    assertTrue(lineContinuation.get().isEmpty());
  }
}
