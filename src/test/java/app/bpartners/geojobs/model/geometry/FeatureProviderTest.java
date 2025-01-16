package app.bpartners.geojobs.model.geometry;

import static app.bpartners.geojobs.model.geometry.plot.PlotConf.DEFAULT_OFFSET;
import static java.awt.Color.BLACK;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.model.geometry.plot.AreImagesEqual;
import app.bpartners.geojobs.model.geometry.plot.PlotConf;
import app.bpartners.geojobs.model.geometry.plot.PlotablePlane;
import java.awt.*;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

public class FeatureProviderTest {
  AreImagesEqual areImagesEqual = new AreImagesEqual(0.00005);
  FeatureProvider lineProvider =
      new FeatureProvider("/geometry/vgg/rond-point.json", true, new IntXY(1024, 1024));
  FeatureProvider pathProvider =
      new FeatureProvider("/geometry/vgg/pathway.json", true, new IntXY(1024, 1024));

  @Test
  void rond_point_is_displayed() throws IOException {
    var toUnify = lineProvider.getPolygons();

    var toUnifyImage =
        new PlotablePlane(1_024, 1_024)
            .plotPolygons(toUnify, new PlotConf(BLACK, new BasicStroke(1), 0.1, DEFAULT_OFFSET));

    var expectedInput =
        ImageIO.read(this.getClass().getResourceAsStream("/geometry/vgg/rond-point.png"));
    assertTrue(areImagesEqual.apply(expectedInput, toUnifyImage));
  }

  @Test
  void pathway_is_displayed() throws IOException {
    var featuresGeometry = pathProvider.getPolygons();

    var toUnifyImage =
        new PlotablePlane(512, 512)
            .plotPolygons(
                featuresGeometry,
                new PlotConf(BLACK, new BasicStroke(1), 0.1, new IntXY(2_000, 1_200)));

    var expectedInput =
        ImageIO.read(this.getClass().getResourceAsStream("/geometry/vgg/pathway.png"));
    assertTrue(areImagesEqual.apply(expectedInput, toUnifyImage));
  }
}
