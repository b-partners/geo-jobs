package app.bpartners.geojobs.model.geometry.route;

import app.bpartners.geojobs.model.geometry.FeatureProvider;
import app.bpartners.geojobs.model.geometry.IntXY;
import app.bpartners.geojobs.model.geometry.plot.AreImagesEqual;
import app.bpartners.geojobs.model.geometry.plot.PlotConf;
import app.bpartners.geojobs.model.geometry.plot.Plotable;
import app.bpartners.geojobs.model.geometry.plot.PlotablePlane;
import app.bpartners.geojobs.model.geometry.plot.PlotablePolygon;
import app.bpartners.geojobs.model.geometry.quadrilateral.model.AlphaConf;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Set;

import static app.bpartners.geojobs.model.geometry.plot.PlotConf.DEFAULT_STROKE;
import static java.awt.Color.BLACK;
import static java.awt.Color.GREEN;
import static java.awt.Color.RED;
import static java.lang.Math.PI;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoutesContinuationTest {

  AreImagesEqual areImagesEqual = new AreImagesEqual(0.005); // note(numeric-instability)
  FeatureProvider rondPointFeatureProvider =
      new FeatureProvider("/geometry/vgg/rond-point.json", true, new IntXY(1024, 1024));

  private AlphaConf alphaConf() {
    return new AlphaConf(0.5 /*note(alpha-minCoverage)*/, 1);
  }

  private UnionConf unionConf() {
    return new UnionConf(5);
  }

  private ContinuationConf continuationConf() {
    return new ContinuationConf(PI / 6, 500);
  }

  @Test
  void rond_point_continuations_with_details() throws IOException {
    var polygons = rondPointFeatureProvider.getPolygons();
    var scale = 0.1;
    var offset = new IntXY(2_000, 1_000);
    var expectedImage =
        ImageIO.read(
            this.getClass().getResourceAsStream("/geometry/vgg/rond-point-continuations.png"));

    areContinuationsCorrectWithDetails(polygons, scale, offset, expectedImage);
  }

  @Test
  void rond_point_continued() throws IOException {
    var polygons = rondPointFeatureProvider.getPolygons();
    var scale = 0.1;
    var offset = new IntXY(2_000, 1_000);
    var expectedImage =
        ImageIO.read(this.getClass().getResourceAsStream("/geometry/vgg/rond-point-continued.png"));

    isContinuedCorrect(polygons, scale, offset, expectedImage);
  }

  @Test
  void t_like_continuations_with_details() throws IOException {
    var polygons = rondPointFeatureProvider.getPolygons();
    var scale = 0.1;
    var offset = new IntXY(-221_000, 109_000);
    var expectedImage =
        ImageIO.read(this.getClass().getResourceAsStream("/geometry/vgg/t-like-continuations.png"));

    areContinuationsCorrectWithDetails(polygons, scale, offset, expectedImage);
  }

  @Test
  void t_like_point_continued() throws IOException {
    var polygons = rondPointFeatureProvider.getPolygons();
    var scale = 0.1;
    var offset = new IntXY(-221_000, 109_000);
    var expectedImage =
        ImageIO.read(this.getClass().getResourceAsStream("/geometry/vgg/t-like-continued.png"));

    isContinuedCorrect(polygons, scale, offset, expectedImage);
  }

  private void areContinuationsCorrectWithDetails(
      Set<org.locationtech.jts.geom.Polygon> polygons,
      double scale,
      IntXY offset,
      BufferedImage expectedImage) {
    var alphaConf = alphaConf();
    var continuationConf = continuationConf();
    var unionConf = unionConf();
    var continuations = new RoutesContinuation(polygons, alphaConf, unionConf, continuationConf);
    Set<Plotable> plotables =
        continuations.continuations().stream()
            .map(
                p -> new PlotablePolygon(p, new PlotConf(GREEN, new BasicStroke(10), scale, offset)))
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
    assertTrue(areImagesEqual.apply(expectedImage, actualImage));
  }

  private void isContinuedCorrect(
      Set<org.locationtech.jts.geom.Polygon> polygons,
      double scale,
      IntXY offset,
      BufferedImage expectedImage) {
    var alphaConf = alphaConf();
    var continuationConf = continuationConf();
    var unionConf = unionConf();
    var continuations = new RoutesContinuation(polygons, alphaConf, unionConf, continuationConf);
    Set<Plotable> plotables =
        continuations.continued().stream()
            .map(
                p -> new PlotablePolygon(p, new PlotConf(BLACK, new BasicStroke(4), scale, offset)))
            .collect(toSet());

    var actualImage = new PlotablePlane(1_024, 1_024).plot(plotables);
    assertTrue(areImagesEqual.apply(expectedImage, actualImage));
  }
}
