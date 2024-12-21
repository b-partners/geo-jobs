package app.bpartners.geojobs.model.geometry.plot;

import static app.bpartners.geojobs.model.geometry.TestData.compass1Polygon;
import static app.bpartners.geojobs.model.geometry.TestData.croissant1Polygon;
import static app.bpartners.geojobs.model.geometry.TestData.croissant2Polygon;
import static app.bpartners.geojobs.model.geometry.TestData.longPolygon;
import static java.awt.Color.BLACK;
import static java.awt.Color.RED;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.model.geometry.quadrilateral.Quadrilateral;
import java.io.IOException;
import java.util.Set;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;

class PlotablePlaneTest {

  AreImagesEqual areImagesEqual =
      new AreImagesEqual(
          // note(numeric-instability): there are numeric stability problems that occur
          // non-deterministically
          0.005);

  @Test
  void plot_quadrilaterals() throws IOException {
    var expected =
        ImageIO.read(this.getClass().getResourceAsStream("/geometry/two-quadrilaterals.png"));

    var actual =
        new PlotablePlane(1024, 1024)
            .plotQuadrilaterals(
                Set.of(
                    new Quadrilateral(
                        Set.of(
                            new Coordinate(10, 10),
                            new Coordinate(300, 100),
                            new Coordinate(100, 200),
                            new Coordinate(400, 400))),
                    new Quadrilateral(
                        Set.of(
                            new Coordinate(500, 510),
                            new Coordinate(400, 700),
                            new Coordinate(910, 1000),
                            new Coordinate(800, 690)))));

    assertTrue(areImagesEqual.apply(expected, actual));
  }

  @Test
  void plot_polygon() throws IOException {
    var expected = ImageIO.read(this.getClass().getResourceAsStream("/geometry/longPolygon.png"));

    var actual = new PlotablePlane(512, 512).plot(Set.of(new PlotablePolygon(longPolygon(), RED)));

    assertTrue(areImagesEqual.apply(expected, actual));
  }

  @Test
  void plot_croissant1() throws IOException {
    var expected =
        ImageIO.read(this.getClass().getResourceAsStream("/geometry/croissant1Polygon.png"));

    var actual =
        new PlotablePlane(512, 512).plot(Set.of(new PlotablePolygon(croissant1Polygon(), BLACK)));

    assertTrue(areImagesEqual.apply(expected, actual));
  }

  @Test
  void plot_croissant2() throws IOException {
    var expected =
        ImageIO.read(this.getClass().getResourceAsStream("/geometry/croissant1Polygon.png"));

    var actual =
        new PlotablePlane(512, 512).plot(Set.of(new PlotablePolygon(croissant2Polygon(), BLACK)));

    assertTrue(areImagesEqual.apply(expected, actual));
  }

  @Test
  void plot_compass1() throws IOException {
    var expected =
        ImageIO.read(this.getClass().getResourceAsStream("/geometry/compass1Polygon.png"));

    var actual =
        new PlotablePlane(1024, 1024).plot(Set.of(new PlotablePolygon(compass1Polygon(), BLACK)));

    assertTrue(areImagesEqual.apply(expected, actual));
  }
}
