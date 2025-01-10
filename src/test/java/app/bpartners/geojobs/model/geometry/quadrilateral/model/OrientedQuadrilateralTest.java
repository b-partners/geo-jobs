package app.bpartners.geojobs.model.geometry.quadrilateral.model;

import static app.bpartners.geojobs.model.geometry.TestData.quadrilateral1;
import static app.bpartners.geojobs.model.geometry.TestData.quadrilateral2;
import static app.bpartners.geojobs.model.geometry.quadrilateral.model.Orientation.length;
import static java.lang.Math.PI;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.model.geometry.plot.AreImagesEqual;
import app.bpartners.geojobs.model.geometry.plot.PlotablePlane;
import java.io.IOException;
import java.util.Set;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class OrientedQuadrilateralTest {
  AreImagesEqual areImagesEqual = new AreImagesEqual(0.0001); // note(numeric-instability)

  @Test
  void do_not_continue_with_q1_and_q2() throws IOException {
    var distanceThreshold = 200;
    var expected =
        ImageIO.read(this.getClass().getResourceAsStream("/geometry/q1-q2-continued.png"));

    var q1 = new OrientedQuadrilateral(quadrilateral1(), length);
    var q2 = new OrientedQuadrilateral(quadrilateral2(), length);
    var continuation = q1.continueWith(q2, PI / 50, distanceThreshold);
    assertTrue(continuation.isEmpty());
  }

  @Test
  void continue_with_q1_and_q2() throws IOException {
    var distanceThreshold = 200;
    var expected =
        ImageIO.read(this.getClass().getResourceAsStream("/geometry/q1-q2-continued.png"));

    var q1 = new OrientedQuadrilateral(quadrilateral1(), length);
    var q2 = new OrientedQuadrilateral(quadrilateral2(), length);
    var continuation = q1.continueWith(q2, PI / 40, distanceThreshold).get();
    var actual =
        new PlotablePlane(1024, 1024)
            .plotQuadrilaterals(
                Set.of(q1.quadrilateral(), q2.quadrilateral(), continuation.quadrilateral()));

    assertTrue(areImagesEqual.apply(expected, actual));
  }

  @Test
  void continue_with_q2_and_q1() throws IOException {
    var distanceThreshold = 200;
    var expected =
        ImageIO.read(this.getClass().getResourceAsStream("/geometry/q1-q2-continued.png"));

    var q1 = new OrientedQuadrilateral(quadrilateral1(), length);
    var q2 = new OrientedQuadrilateral(quadrilateral2(), length);
    var continuation = q2.continueWith(q1, PI / 40, distanceThreshold).get();
    var actual =
        new PlotablePlane(1024, 1024)
            .plotQuadrilaterals(
                Set.of(q1.quadrilateral(), q2.quadrilateral(), continuation.quadrilateral()));

    assertTrue(areImagesEqual.apply(expected, actual));
  }
}
