package app.bpartners.geojobs.model.geometry.line;

import app.bpartners.geojobs.model.geometry.plot.AreImagesEqual;
import app.bpartners.geojobs.model.geometry.plot.PlotablePlane;
import java.awt.Color;
import java.io.IOException;
import java.util.Set;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

import static app.bpartners.geojobs.model.geometry.TestData.compass1Polygon;
import static app.bpartners.geojobs.model.geometry.TestData.longPolygon;
import static java.awt.Color.BLACK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UnifiedLineTest {
  AreImagesEqual areImagesEqual = new AreImagesEqual(0.00005);
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
}
