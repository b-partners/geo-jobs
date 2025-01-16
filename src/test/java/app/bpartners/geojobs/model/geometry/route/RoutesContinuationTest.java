package app.bpartners.geojobs.model.geometry.route;

import static app.bpartners.geojobs.model.geometry.plot.PlotConf.DEFAULT_STROKE;
import static java.awt.Color.BLACK;
import static java.awt.Color.GREEN;
import static java.awt.Color.RED;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.model.geometry.FeatureProvider;
import app.bpartners.geojobs.model.geometry.IntXY;
import app.bpartners.geojobs.model.geometry.plot.AreImagesEqual;
import app.bpartners.geojobs.model.geometry.plot.PlotConf;
import app.bpartners.geojobs.model.geometry.plot.Plotable;
import app.bpartners.geojobs.model.geometry.plot.PlotablePlane;
import app.bpartners.geojobs.model.geometry.plot.PlotablePolygon;
import app.bpartners.geojobs.model.geometry.quadrilateral.model.AlphaConf;
import java.awt.*;
import java.io.IOException;
import java.util.Set;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class RoutesContinuationTest {

  AreImagesEqual areImagesEqual = new AreImagesEqual(0.005); // note(numeric-instability)
  FeatureProvider rondPointFeatureProvider =
      new FeatureProvider("/geometry/vgg/rond-point.json", true, new IntXY(1024, 1024));

  @Test
  void rond_point_continuations_with_details() throws IOException {
    var polygons = rondPointFeatureProvider.getPolygons();
    var scale = 0.1;
    var offset = new IntXY(2_000, 1_000);

    var alphaConf = new AlphaConf(0.5 /*note(alpha-minCoverage)*/, 1);
    var continuationConf = new ContinuationConf(0.4, 500);
    var unionConf = new UnionConf(5);
    var continuations = new RoutesContinuation(polygons, alphaConf, unionConf, continuationConf);
    Set<Plotable> plotables =
        continuations.continuations().stream()
            .map(
                p -> new PlotablePolygon(p, new PlotConf(GREEN, new BasicStroke(4), scale, offset)))
            .collect(toSet());

    plotables.addAll(
        polygons.stream()
            .map(p -> new PlotablePolygon(p, new PlotConf(BLACK, DEFAULT_STROKE, scale, offset)))
            .collect(toSet()));
    plotables.addAll(
        continuations.abstractions().stream()
            .flatMap(abstractRoute -> abstractRoute.abstraction().stream())
            .map(oq -> oq.quadrilateral().polygon())
            .map(p -> new PlotablePolygon(p, new PlotConf(RED, DEFAULT_STROKE, scale, offset)))
            .collect(toSet()));
    var actualImage = new PlotablePlane(1_024, 1_024).plot(plotables);
    var expectedImage =
        ImageIO.read(
            this.getClass().getResourceAsStream("/geometry/vgg/rond-point-continuations.png"));
    assertTrue(areImagesEqual.apply(expectedImage, actualImage));
  }

  @Test
  void rond_point_continued() throws IOException {
    var polygons = rondPointFeatureProvider.getPolygons();
    var scale = 0.1;
    var offset = new IntXY(2_000, 1_000);

    var alphaConf = new AlphaConf(0.5 /*note(alpha-minCoverage)*/, 1);
    var continuationConf = new ContinuationConf(0.4, 500);
    var unionConf = new UnionConf(5);
    var continuations = new RoutesContinuation(polygons, alphaConf, unionConf, continuationConf);
    Set<Plotable> plotables =
        continuations.continued().stream()
            .map(
                p -> new PlotablePolygon(p, new PlotConf(BLACK, new BasicStroke(4), scale, offset)))
            .collect(toSet());

    var actualImage = new PlotablePlane(1_024, 1_024).plot(plotables);
    var expectedImage =
        ImageIO.read(this.getClass().getResourceAsStream("/geometry/vgg/rond-point-continued.png"));
    assertTrue(areImagesEqual.apply(expectedImage, actualImage));
  }
}
