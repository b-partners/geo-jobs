package app.bpartners.geojobs.model.geometry.quadrilateral;

import static app.bpartners.geojobs.model.geometry.TestData.compass1Polygon;
import static java.awt.Color.BLACK;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.model.geometry.FeatureProvider;
import app.bpartners.geojobs.model.geometry.plot.AreImagesEqual;
import app.bpartners.geojobs.model.geometry.plot.Plotable;
import app.bpartners.geojobs.model.geometry.plot.PlotablePlane;
import app.bpartners.geojobs.model.geometry.plot.PlotablePolygon;
import app.bpartners.geojobs.model.geometry.plot.PlotableQuadrilateral;
import app.bpartners.geojobs.model.geometry.quadrilateral.Alpha.AlphaConf;
import app.bpartners.geojobs.model.geometry.quadrilateral.model.OrientedQuadrilateral;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import javax.imageio.ImageIO;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Polygon;

class AlphaTest {
  AreImagesEqual areImagesEqual = new AreImagesEqual(0.005); // note(numeric-instability)
  FeatureProvider featureProvider = new FeatureProvider();

  @Test
  void vgg1() {
    isAbstractionCorrect(featureProvider.apply(1), "/geometry/vgg/vgg1-alpha.png");
  }

  @Test
  void vgg2() {
    isAbstractionCorrect(featureProvider.apply(2), "/geometry/vgg/vgg2-alpha-ko.png");
  }

  @Test
  void vgg4() {
    isAbstractionCorrect(featureProvider.apply(4), "/geometry/vgg/vgg4-alpha.png");
  }

  @Test
  void vgg5() {
    isAbstractionCorrect(featureProvider.apply(5), "/geometry/vgg/vgg5-alpha.png");
  }

  @Test
  void vgg11() {
    isAbstractionCorrect(featureProvider.apply(11), "/geometry/vgg/vgg11-alpha.png");
  }

  @Test
  void vgg22() {
    isAbstractionCorrect(featureProvider.apply(22), "/geometry/vgg/vgg22-alpha-can-be-problematic.png");
  }

  @Test
  void vgg101() {
    isAbstractionCorrect(featureProvider.apply(101), "/geometry/vgg/vgg101-alpha.png");
  }

  @Test
  void vgg247() {
    isAbstractionCorrect(featureProvider.apply(247), "/geometry/vgg/vgg247-alpha.png");
  }

  @Test
  void abstraction_failures_are_infrequent_enough() {
    var featuresNb = featureProvider.featuresNb();
    var failedN = new ArrayList<>();
    for (int n = 0; n < featuresNb; n++) {
      System.out.println(n);
      var alpha = alpha(featureProvider.apply(n));
      if (alpha.get().isEmpty()) {
        failedN.add(n);
      }
    }

    assertEquals(324, featuresNb);
    assertEquals(84, failedN.size());
  }

  @Test
  void compass1() {
    isAbstractionCorrect(compass1Polygon(), "/geometry/compass1-alpha.png");
  }

  private void isAbstractionCorrect(Polygon polygon, String pathOfExpectedImage) {
    BufferedImage expected = readImage(pathOfExpectedImage);

    Set<Plotable> plotables = new HashSet<>();
    plotables.add(new PlotablePolygon(polygon, BLACK));
    var alpha = alpha(polygon);
    plotables.addAll(
        alpha.get().stream()
            .map(OrientedQuadrilateral::quadrilateral)
            .map(PlotableQuadrilateral::new)
            .collect(toSet()));
    var actual = new PlotablePlane(1024, 1024).plot(plotables);

    assertTrue(areImagesEqual.apply(expected, actual));
  }

  private static Alpha alpha(Polygon p) {
    return new Alpha(p, new AlphaConf(0.95, 100));
  }

  @SneakyThrows
  private BufferedImage readImage(String path) {
    return ImageIO.read(this.getClass().getResourceAsStream(path));
  }
}
