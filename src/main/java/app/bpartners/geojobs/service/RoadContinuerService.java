package app.bpartners.geojobs.service;

import app.bpartners.geojobs.endpoint.rest.postprocessing.Geojson;
import app.bpartners.geojobs.endpoint.rest.postprocessing.continuer.LatLonLinesContinuer;
import app.bpartners.geojobs.endpoint.rest.postprocessing.continuer.confFactory.ContinuationConfFactory;
import app.bpartners.geojobs.endpoint.rest.postprocessing.continuer.confFactory.PrettyConfFactory;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TilingConf;
import app.bpartners.geojobs.model.geometry.quadrilateral.model.AlphaConf;
import app.bpartners.geojobs.model.geometry.route.ContinuationConf;
import app.bpartners.geojobs.model.geometry.route.PrettyConf;
import app.bpartners.geojobs.model.geometry.route.RoutesContinuationConf;
import app.bpartners.geojobs.model.geometry.route.UnionConf;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class RoadContinuerService {
  private static final AlphaConf DEFAULT_ALPHA_CONF = new AlphaConf(0.55d, 1);
  private static final UnionConf DEFAULT_UNION_CONF = new UnionConf(1);

  public Geojson continueRoute(String geojsonString, TilingConf tilingConf) throws IOException {
    var toBeContinuedFile = getGeoJsonFromString(geojsonString);
    var toBeContinuedGeoJSON = new Geojson(toBeContinuedFile);

    var continuer = getLatLonContinuer(getRouteContinuationConf(toBeContinuedGeoJSON), tilingConf);
    return new Geojson(continuer.apply(toBeContinuedFile));
  }

  public static File getGeoJsonFromString(String geoJsonString) throws IOException {
    File tempFile = File.createTempFile("geojson-", ".geojson");
    tempFile.deleteOnExit();

    ObjectMapper mapper = new ObjectMapper();
    mapper.writeValue(tempFile, geoJsonString);

    return tempFile;
  }

  private static LatLonLinesContinuer getLatLonContinuer(
      RoutesContinuationConf routesContinuationConf, TilingConf tilingConf) {
    return new LatLonLinesContinuer(routesContinuationConf, tilingConf, 10);
  }

  private static RoutesContinuationConf getRouteContinuationConf(Geojson geojson) {
    ContinuationConf continuationConf = new ContinuationConfFactory().apply(geojson.polygons());
    PrettyConf prettyConf = new PrettyConfFactory().apply(geojson.polygons());

    return new RoutesContinuationConf(
        DEFAULT_ALPHA_CONF, DEFAULT_UNION_CONF, continuationConf, prettyConf);
  }
}
