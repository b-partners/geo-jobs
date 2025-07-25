package app.bpartners.geojobs.endpoint.rest.postprocessing.continuer;

import static java.lang.Math.PI;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.endpoint.rest.postprocessing.Geojson;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TilingConf;
import app.bpartners.geojobs.model.geometry.quadrilateral.model.AlphaConf;
import app.bpartners.geojobs.model.geometry.route.ContinuationConf;
import app.bpartners.geojobs.model.geometry.route.PrettyConf;
import app.bpartners.geojobs.model.geometry.route.RoutesContinuationConf;
import app.bpartners.geojobs.model.geometry.route.UnionConf;
import app.bpartners.geojobs.service.geojson.GeoJsonContinuerService;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class LatLonLinesContinuerTest {
  GeoJsonContinuerService geoJsonContinuerService = new GeoJsonContinuerService();

  @Test
  void continue_ivandry() throws IOException, URISyntaxException {
    var ivandryGeojson =
        new File(getClass().getResource("/ivandry/route-ivandry.geojson").getFile());

    var routesContinuationConf = routesContinuationConf();
    var tilingConf = new TilingConf(17, 1_024);
    var latLonLinesContinuer = new LatLonLinesContinuer(routesContinuationConf, tilingConf, 10);
    var continued = latLonLinesContinuer.apply(ivandryGeojson);

    var expectedURI =
        Paths.get(getClass().getResource("/ivandry/route-ivandry-continued.geojson").toURI());
    var expected = Files.readString(expectedURI);
    // assertEquals(expected, new Geojson(continued).stringValue());
  }

  @Test
  void continue_amboditsiry() throws IOException, URISyntaxException {
    var actualGeojson =
        new File(getClass().getResource("/amboditsiry/route-amboditsiry.geojson").getFile());

    var routesContinuationConf = routesContinuationConf();
    var tilingConf = new TilingConf(17, 1_024);
    var latLongLinesContinuer = new LatLonLinesContinuer(routesContinuationConf, tilingConf, 10);
    var continued = latLongLinesContinuer.apply(actualGeojson);
    var expectedURI =
        Paths.get(
            getClass().getResource("/amboditsiry/route-amboditsiry-continued.geojson").toURI());
    var expectedGeojson =
        new File(
            getClass().getResource("/amboditsiry/route-amboditsiry-continued.geojson").getFile());
    var expected = new Geojson(expectedGeojson);
    var expectedContinued = Files.readString(expectedURI);

    assertTrue(continued.size() <= expected.polygons().size());
    assertEquals(expectedContinued, new Geojson(continued).stringValue());
  }

  private static RoutesContinuationConf routesContinuationConf() {
    var alphaConf = new AlphaConf(0.5, 1);
    var unionConf = new UnionConf(1);
    var continuationConf = new ContinuationConf(PI / 12, PI / 6, 500);
    var prettyConf = new PrettyConf(0);
    return new RoutesContinuationConf(alphaConf, unionConf, continuationConf, prettyConf);
  }

  @Test
  void continue_service_test() throws IOException, URISyntaxException {
    GeoJsonContinuerService geoJsonContinuerService = new GeoJsonContinuerService();

    File input = new File(getClass().getResource("/amboditsiry/route-amboditsiry.geojson").getFile());
    var expectedURI =
            Paths.get(
                    getClass().getResource("/amboditsiry/route-amboditsiry-continued.geojson").toURI());

    var actual = geoJsonContinuerService.continueGeojson(input);
    var expectedContinued = Files.readString(expectedURI);

    assertEquals(expectedContinued, actual.stringValue());
    assertTrue(actual.polygons().size() > 0);
  }
}
