package app.bpartners.geojobs.model.geometry.line;

import static app.bpartners.geojobs.model.geometry.TestData.compass1Polygon;
import static app.bpartners.geojobs.model.geometry.TestData.longPolygon;
import static java.awt.Color.BLACK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.model.geometry.FeatureProvider;
import app.bpartners.geojobs.model.geometry.plot.AreImagesEqual;
import app.bpartners.geojobs.model.geometry.plot.PlotablePlane;
import java.io.IOException;
import java.util.Set;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

public class UnifiedLineTest {
  AreImagesEqual areImagesEqual = new AreImagesEqual(0.00005);
  FeatureProvider featureProvider = new FeatureProvider("/geometry/vgg/line.json");

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
  void vgg_line_unified() throws IOException {
    var expected =
        ImageIO.read(this.getClass().getResourceAsStream("/geometry/long2-compass2-continued.png"));
    var toUnify = featureProvider.getFeaturesGeometry();

    var unifiedLine = new UnifiedLine(toUnify);
    var actual = new PlotablePlane(1024, 1024).plot(unifiedLine.unified(), BLACK);

    assertTrue(areImagesEqual.apply(expected, actual));
  }
}
