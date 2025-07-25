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
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class RoadContinuerService {
  private static final AlphaConf DEFAULT_ALPHA_CONF = new AlphaConf(0.55d, 1);
  private static final UnionConf DEFAULT_UNION_CONF = new UnionConf(1);

  @SneakyThrows
  public File continueRoute(String geojsonString, TilingConf tilingConf) {
    File toBeContinuedFile = getGeoJsonFromString(geojsonString);
    Geojson toBeContinuedGeoJSON = new Geojson(toBeContinuedFile);

    LatLonLinesContinuer continuer =
        getLatLonContinuer(getRouteContinuationConf(toBeContinuedGeoJSON), tilingConf);
    var continuedPolygons = continuer.apply(toBeContinuedFile);

    return getGeoJsonFromString(new Geojson(continuedPolygons).stringValue());
  }

  public TilingConf getTilingConf(Integer zoom, Integer imgSize) {
    int fZoom = (zoom == null) ? 20 : zoom;
    int fImgSize = (imgSize == null) ? 1_024 : imgSize;

    return new TilingConf(fZoom, fImgSize);
  }

  public static File getGeoJsonFromString(String geoJsonString) throws IOException {
    String uuidName = UUID.randomUUID().toString();
    File tempFile = File.createTempFile("continued-geojson-" + uuidName, ".geojson");

    Files.writeString(tempFile.toPath(), geoJsonString);
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
