package app.bpartners.geojobs.service;

import static java.lang.Math.PI;

import app.bpartners.geojobs.endpoint.rest.postprocessing.GeoJsonValidator;
import app.bpartners.geojobs.endpoint.rest.postprocessing.Geojson;
import app.bpartners.geojobs.endpoint.rest.postprocessing.continuer.LatLonLinesContinuer;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TilingConf;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.model.geometry.quadrilateral.model.AlphaConf;
import app.bpartners.geojobs.model.geometry.route.ContinuationConf;
import app.bpartners.geojobs.model.geometry.route.PrettyConf;
import app.bpartners.geojobs.model.geometry.route.RoutesContinuationConf;
import app.bpartners.geojobs.model.geometry.route.UnionConf;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class RoadContinuerService {
  private static final AlphaConf DEFAULT_ALPHA_CONF = new AlphaConf(0.5d, 1);
  private static final UnionConf DEFAULT_UNION_CONF = new UnionConf(1);
  private static final PrettyConf DEFAULT_PRETTY_CONF = new PrettyConf(0);
  private static final int DEFAULT_NEIGHBOUR_THRESHOLD = 10;
  private static final ContinuationConf DEFAULT_CONTINUATION_CONF =
      new ContinuationConf(PI / 12, PI / 6, 500);

  private final BucketComponent bucketComponent;
  private final GeoJsonValidator geoJsonValidator;

  public Map<String, String> continueRoute(String geojsonString, Integer zoom, Integer imgSize) {
    var tilingConf = getTilingConf(zoom, imgSize);
    try {
      File toBeContinuedFile = getGeoJsonFromString(geojsonString);
      if (geoJsonValidator.test(toBeContinuedFile)) {
        LatLonLinesContinuer continuer = getLatLonContinuer(getRouteContinuationConf(), tilingConf);
        var continuedPolygons = continuer.apply(toBeContinuedFile);
        var continuedGeoJsonFile =
            getGeoJsonFromString(new Geojson(continuedPolygons).stringValue());

        return getPresignedURL(continuedGeoJsonFile);
      }
      throw new RuntimeException("Could not proceed to polygon road continuation");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private Map<String, String> getPresignedURL(File continuedGeoJsonFile) {
    var bucketKey = "continuedRoads/" + UUID.randomUUID() + ".geojson";
    bucketComponent.upload(continuedGeoJsonFile, bucketKey);
    String presignURL = bucketComponent.presign(bucketKey);

    return Map.of("url", presignURL);
  }

  public TilingConf getTilingConf(Integer zoom, Integer imgSize) {
    var defaultConf = TilingConf.getDefaultInstance();
    int fZoom = (zoom == null) ? defaultConf.z() : zoom;
    int fImgSize = (imgSize == null) ? defaultConf.imgSize() : imgSize;

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
    return new LatLonLinesContinuer(
        routesContinuationConf, tilingConf, DEFAULT_NEIGHBOUR_THRESHOLD);
  }

  private static RoutesContinuationConf getRouteContinuationConf() {
    return new RoutesContinuationConf(
        DEFAULT_ALPHA_CONF, DEFAULT_UNION_CONF, DEFAULT_CONTINUATION_CONF, DEFAULT_PRETTY_CONF);
  }
}
