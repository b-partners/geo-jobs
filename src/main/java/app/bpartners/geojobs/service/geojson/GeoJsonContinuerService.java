package app.bpartners.geojobs.service.geojson;

import static java.lang.Math.PI;

import app.bpartners.geojobs.endpoint.rest.postprocessing.Geojson;
import app.bpartners.geojobs.endpoint.rest.postprocessing.continuer.LatLonLinesContinuer;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.LatLonPolygon;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TilingConf;
import app.bpartners.geojobs.model.geometry.quadrilateral.model.AlphaConf;
import app.bpartners.geojobs.model.geometry.route.ContinuationConf;
import app.bpartners.geojobs.model.geometry.route.PrettyConf;
import app.bpartners.geojobs.model.geometry.route.RoutesContinuationConf;
import app.bpartners.geojobs.model.geometry.route.UnionConf;
import java.io.File;
import java.util.Set;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class GeoJsonContinuerService {
  private final RoutesContinuationConf routesContinuationConf = routesContinuationConf();
  private final LatLonLinesContinuer latLonLinesContinuer =
      new LatLonLinesContinuer(routesContinuationConf, new TilingConf(17, 1_024), 10);

  public Geojson continueGeojson(File geoJsonToContinue) {
    Set<LatLonPolygon> features = latLonLinesContinuer.apply(geoJsonToContinue);
    return new Geojson(features);
  }

  private static RoutesContinuationConf routesContinuationConf() {
    var alphaConf = new AlphaConf(0.5, 1);
    var unionConf = new UnionConf(1);
    var continuationConf = new ContinuationConf(PI / 12, PI / 6, 500);
    var prettyConf = new PrettyConf(0);
    return new RoutesContinuationConf(alphaConf, unionConf, continuationConf, prettyConf);
  }
}
