package app.bpartners.geojobs.model.geometry;

import static java.awt.Color.BLACK;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.model.geometry.plot.AreImagesEqual;
import app.bpartners.geojobs.model.geometry.plot.PlotablePlane;
import java.awt.*;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

public class FeatureProviderTest {
  AreImagesEqual areImagesEqual = new AreImagesEqual(0.00005);
  FeatureProvider featureProvider =
      new FeatureProvider("/geometry/vgg/rond-point.json", true, new IntXY(1024, 1024));

  @Test
  void rond_point_is_displayed() throws IOException {
    var toUnify = featureProvider.getFeaturesGeometry();

    var toUnifyImage =
        new PlotablePlane(1_024, 1_024).plot(toUnify, BLACK, new BasicStroke(1), 0.1);

    var expectedInput =
        ImageIO.read(this.getClass().getResourceAsStream("/geometry/vgg/rond-point.png"));
    assertTrue(areImagesEqual.apply(expectedInput, toUnifyImage));
  }
}
