package app.bpartners.geojobs.endpoint.rest.postprocessing.continuer;

import static java.lang.Math.PI;
import static org.junit.jupiter.api.Assertions.*;

import app.bpartners.geojobs.endpoint.rest.postprocessing.Geojson;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TilingConf;
import app.bpartners.geojobs.model.geometry.quadrilateral.model.AlphaConf;
import app.bpartners.geojobs.model.geometry.route.ContinuationConf;
import app.bpartners.geojobs.model.geometry.route.PrettyConf;
import app.bpartners.geojobs.model.geometry.route.RoutesContinuationConf;
import app.bpartners.geojobs.model.geometry.route.UnionConf;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class LatLonLinesContinuerTest {
  private final TilingConf tilingConf = new TilingConf(17, 1_024);

  @Test
  void continue_ivandry() throws IOException, URISyntaxException {
    var ivandryGeojson =
        new File(getClass().getResource("/ivandry/route-ivandry.geojson").getFile());

    var routesContinuationConf = routesContinuationConf();
    var latLonLinesContinuer = new LatLonLinesContinuer(routesContinuationConf, tilingConf, 10);
    var continued = latLonLinesContinuer.apply(ivandryGeojson);

    var expectedURI =
        Paths.get(getClass().getResource("/ivandry/route-ivandry-continued.geojson").toURI());
    var expected = Files.readString(expectedURI);
    int expectedPolygonNumber = 1;

    assertEquals(expectedPolygonNumber, new Geojson(continued).polygons().size());
  }

  @Test
  void continue_anosy_rond_point() throws URISyntaxException {
    var subjectResource = getClass().getResource("/geojson/anosy-rond-point.geojson");

    assertNotNull(subjectResource);

    var subjectGeojsonFile = new File(subjectResource.toURI());

    var routesContinuationConf = routesContinuationConf();

    var latLonLinesContinuer = new LatLonLinesContinuer(routesContinuationConf, tilingConf, 10);
    var continued = latLonLinesContinuer.apply(subjectGeojsonFile);

    int actualPolygonSize = new Geojson(continued).polygons().size();
    int originalPolygonSize = new Geojson(subjectGeojsonFile).polygons().size();

    assertTrue(0 < actualPolygonSize && actualPolygonSize <= originalPolygonSize);
  }

  @Test
  void continue_ambohijatovo_intersection() throws URISyntaxException {
    var subjectResource = getClass().getResource("/geojson/ambohijatovo-crossed.geojson");

    assertNotNull(subjectResource);

    File subjectFile = new File(subjectResource.toURI());
    RoutesContinuationConf routesContinuationConf = routesContinuationConf();

    var subject = new LatLonLinesContinuer(routesContinuationConf, tilingConf, 10);

    var originalPolygon = new Geojson(subjectFile);
    var continuedPolygon = subject.apply(subjectFile);

    var actual = new Geojson(continuedPolygon);

    var originalPolygonSize = originalPolygon.polygons().size();
    var actualPolygonSize = actual.polygons().size();

    assertTrue(actualPolygonSize <= originalPolygonSize);
    assertTrue(0 < actualPolygonSize);
  }

  @Test
  void continue_anosy_rond_point2() throws URISyntaxException {
    var subjectResource = getClass().getResource("/geojson/anosy-rond-point-2.geojson");

    assertNotNull(subjectResource);

    File subjectGeojsonFile = new File(subjectResource.toURI());
    var routesContinuationConf = routesContinuationConf();

    var latLonLinesContinuer = new LatLonLinesContinuer(routesContinuationConf, tilingConf, 10);
    var continued = latLonLinesContinuer.apply(subjectGeojsonFile);

    var originalPolygonSize = new Geojson(subjectGeojsonFile).polygons().size();
    var actual = new Geojson(continued);
    var actualPolygonSize = actual.polygons().size();

    assertTrue(0 < actualPolygonSize && actualPolygonSize <= originalPolygonSize);
  }

  @Test
  void continue_boulevard_saint_bernard_paris() throws URISyntaxException {
    var subjectResource = getClass().getResource("/geojson/boulevard-saint-benard.geojson");

    assertNotNull(subjectResource);

    File subjectGeojsonFile = new File(subjectResource.toURI());

    RoutesContinuationConf routesContinuationConf = routesContinuationConf();

    var subject = new LatLonLinesContinuer(routesContinuationConf, tilingConf, 10);
    var continued = subject.apply(subjectGeojsonFile);

    int originalPolygoSize = new Geojson(subjectGeojsonFile).polygons().size();
    int actualPolygonSize = new Geojson(continued).polygons().size();

    assertTrue(0 < actualPolygonSize && actualPolygonSize <= originalPolygoSize);
  }

  @Test
  void continue_quai_de_bourbon_paris() throws URISyntaxException {
    var subjectResource = getClass().getResource("/geojson/quai-de-bourbon.geojson");

    assertNotNull(subjectResource);

    File subjectGeojsonFile = new File(subjectResource.toURI());

    RoutesContinuationConf routesContinuationConf = routesContinuationConf();

    var subject = new LatLonLinesContinuer(routesContinuationConf, tilingConf, 10);

    var continuedPolygons = subject.apply(subjectGeojsonFile);

    var actual = new Geojson(continuedPolygons);

    assertEquals(1, actual.polygons().size());
  }

  @Test
  void continue_pharmacie_saint_vigor_paris() throws URISyntaxException {
    var subjectResource = getClass().getResource("/geojson/pharmacie-saint-vigor.geojson");
    assertNotNull(subjectResource);

    var subjectGeojsonFile = new File(subjectResource.toURI());

    var routesContinuationConf = routesContinuationConf();

    var latLonLinesContinuer = new LatLonLinesContinuer(routesContinuationConf, tilingConf, 10);
    var continued = latLonLinesContinuer.apply(subjectGeojsonFile);

    int originalPolygonSize = new Geojson(subjectGeojsonFile).polygons().size();
    int actualPolygonSize = new Geojson(continued).polygons().size();

    assertTrue(0 < actualPolygonSize && actualPolygonSize <= originalPolygonSize);
  }

  @Test
  void continue_avenue_saint_grenier_paris() throws URISyntaxException {
    var subjectResource = getClass().getResource("/geojson/avenue-pierre-grenier.geojson");

    assertNotNull(subjectResource);

    File subjectGeojsonFile = new File(subjectResource.toURI());

    RoutesContinuationConf routesContinuationConf = routesContinuationConf();

    var latLonLinesContinuer = new LatLonLinesContinuer(routesContinuationConf, tilingConf, 10);
    var continued = latLonLinesContinuer.apply(subjectGeojsonFile);

    int originalPolygonSize = new Geojson(subjectGeojsonFile).polygons().size();
    int actualPolygonSize = new Geojson(continued).polygons().size();

    assertTrue(0 < actualPolygonSize && actualPolygonSize <= originalPolygonSize);
  }

  @Test
  void continue_ampasanimalo_intersection() throws URISyntaxException {
    var subjectResource = getClass().getResource("/geojson/ampasanimalo-intersection.geojson");

    assertNotNull(subjectResource);

    var subjectGeojsonFile = new File(subjectResource.toURI());

    RoutesContinuationConf routesContinuationConf = routesContinuationConf();

    var latLonLinesContinuer = new LatLonLinesContinuer(routesContinuationConf, tilingConf, 10);
    var continued = latLonLinesContinuer.apply(subjectGeojsonFile);

    int originalPolygonSize = new Geojson(subjectGeojsonFile).polygons().size();
    int actualPolygonSize = new Geojson(continued).polygons().size();

    assertTrue(0 < actualPolygonSize && actualPolygonSize <= originalPolygonSize);
  }

  @Test
  void continue_ambohimanjaka() throws URISyntaxException {
    var subjectResource = getClass().getResource("/geojson/ambohimanjaka.geojson");

    assertNotNull(subjectResource);

    var subjectGeojsonFile = new File(subjectResource.toURI());

    RoutesContinuationConf routesContinuationConf = routesContinuationConf();

    var subject = new LatLonLinesContinuer(routesContinuationConf, tilingConf, 10);
    var continued = subject.apply(subjectGeojsonFile);

    int originalPolygonSize = new Geojson(subjectGeojsonFile).polygons().size();
    int actualPolygonSize = new Geojson(continued).polygons().size();

    assertTrue(0 < actualPolygonSize && actualPolygonSize <= originalPolygonSize);
  }

  @Test
  void continue_eglise_saint_eustache_paris() throws URISyntaxException {
    var subjectResource = getClass().getResource("/geojson/eglise-saint-eustache.geojson");

    assertNotNull(subjectResource);

    var subjectGeojsonFile = new File(subjectResource.toURI());

    RoutesContinuationConf routesContinuationConf = routesContinuationConf();

    var subject = new LatLonLinesContinuer(routesContinuationConf, tilingConf, 10);
    var continued = subject.apply(subjectGeojsonFile);

    var actual = new Geojson(continued);

    assertEquals(1, actual.polygons().size());
  }

  private static RoutesContinuationConf routesContinuationConf() {
    var alphaConf = new AlphaConf(0.5, 1);
    var unionConf = new UnionConf(1);
    var continuationConf = new ContinuationConf(PI / 12, PI / 6, 500);
    var prettyConf = new PrettyConf(0);
    return new RoutesContinuationConf(alphaConf, unionConf, continuationConf, prettyConf);
  }
}
