package app.bpartners.geojobs.model.geometry.line;

import static app.bpartners.geojobs.model.geometry.TestData.compass2Polygon;
import static app.bpartners.geojobs.model.geometry.TestData.long2Polygon;
import static java.awt.Color.BLACK;
import static java.lang.Math.PI;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.model.geometry.plot.AreImagesEqual;
import app.bpartners.geojobs.model.geometry.plot.PlotableMultiPolygon;
import app.bpartners.geojobs.model.geometry.plot.PlotablePlane;
import app.bpartners.geojobs.model.geometry.plot.PlotablePolygon;
import app.bpartners.geojobs.model.geometry.quadrilateral.Alpha.AlphaConf;
import java.io.IOException;
import java.util.Set;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

class LineContinuerTest {

  AreImagesEqual areImagesEqual = new AreImagesEqual(0.00005); // note(numeric-instability)

  @Test
  void long2_compass2_coninued() throws IOException {
    var expected =
        ImageIO.read(this.getClass().getResourceAsStream("/geometry/long2-compass2-continued.png"));
    var directionThreshold = PI / 4;
    var alphaConf = new AlphaConf(0.95, 100);
    var lineContinuer = new LineContinuer(directionThreshold, alphaConf);

    var long2 = long2Polygon();
    var compass2 = compass2Polygon();
    var continuation = lineContinuer.apply(compass2, long2);
    var actual =
        new PlotablePlane(1024, 1024)
            .plot(Set.of(new PlotablePolygon((Polygon) continuation, BLACK)));

    assertTrue(areImagesEqual.apply(expected, actual));
  }

  @Test
  void long2_compass2_not_continued() throws IOException {
    var expected =
        ImageIO.read(this.getClass().getResourceAsStream("/geometry/long2-compass2.png"));
    var directionThreshold = PI / 6; // !!!!! Tolerance is too small: direction condition will fail
    var alphaConf = new AlphaConf(0.95, 100);
    var lineContinuer = new LineContinuer(directionThreshold, alphaConf);

    var long2 = long2Polygon();
    var compass2 = compass2Polygon();
    var continuation = lineContinuer.apply(compass2, long2);
    var actual =
        new PlotablePlane(1024, 1024)
            .plot(Set.of(new PlotableMultiPolygon((MultiPolygon) continuation, BLACK)));

    assertTrue(areImagesEqual.apply(expected, actual));
  }
}
