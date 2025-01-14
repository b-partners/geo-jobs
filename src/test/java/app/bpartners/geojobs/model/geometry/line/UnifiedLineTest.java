package app.bpartners.geojobs.model.geometry.line;

import app.bpartners.geojobs.model.geometry.FeatureProvider;
import app.bpartners.geojobs.model.geometry.IntXY;
import app.bpartners.geojobs.model.geometry.plot.AreImagesEqual;
import app.bpartners.geojobs.model.geometry.plot.PlotablePlane;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.IOException;
import java.util.Set;

import static app.bpartners.geojobs.model.geometry.TestData.compass1Polygon;
import static app.bpartners.geojobs.model.geometry.TestData.longPolygon;
import static java.awt.Color.BLACK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UnifiedLineTest {
  AreImagesEqual areImagesEqual = new AreImagesEqual(0.00005);
  FeatureProvider featureProvider =
      new FeatureProvider("/geometry/vgg/rond-point.json", true, new IntXY(1024, 1024));

  @Test
  void long1_compass1_unified() throws IOException {
    var expected =
        ImageIO.read(this.getClass().getResourceAsStream("/geometry/long1-compass1-unified.png"));
    var long1 = longPolygon();
    var compass1 = compass1Polygon();

    var unifiedLine = new UnifiedLine(Set.of(long1, compass1));
    var actual = new PlotablePlane(1024, 1024).plot(unifiedLine.unified(), BLACK);

    assertEquals(1, unifiedLine.unified().size());
    assertTrue(areImagesEqual.apply(expected, actual));
  }

  @Test
  void rond_point_is_unified() throws IOException {
    var toUnify = featureProvider.getFeaturesGeometry();

    var unified = new UnifiedLine(toUnify);
    var unifiedImage =
        new PlotablePlane(1_024, 1_024).plot(unified.unified(), BLACK, new BasicStroke(1), 0.1);

    var expectedOutput =
        ImageIO.read(this.getClass().getResourceAsStream("/geometry/vgg/rond-point.png"));
    assertTrue(areImagesEqual.apply(expectedOutput, unifiedImage));
  }
}
