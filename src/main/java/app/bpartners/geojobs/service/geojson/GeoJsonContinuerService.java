package app.bpartners.geojobs.service.geojson;

import static app.bpartners.geojobs.model.continuationConf.LatLonLinesContinuer.*;
import static app.bpartners.geojobs.model.continuationConf.RoutesContinuationConf.*;
import static java.lang.Math.PI;

import app.bpartners.geojobs.endpoint.rest.postprocessing.Geojson;
import org.springframework.web.multipart.MultipartFile;
@AllArgsConstructor
@Service
public class GeoJsonContinuerService {
  private final RoutesContinuationConf routesContinuationConf = routesContinuationConfVal();
  private final LatLonLinesContinuer latLonLinesContinuer =
      new LatLonLinesContinuer(routesContinuationConf, tilingConfVal(), DEFAULT_NEIGHBOURHOOD.getValue());

  public Geojson continueGeojson(File geoJsonToContinue) {
    Set<LatLonPolygon> features = latLonLinesContinuer.apply(geoJsonToContinue);
    return new Geojson(features);
  }

  private static RoutesContinuationConf routesContinuationConfVal() {
    var alphaConf = new AlphaConf(DEFAULT_MIN_COVERAGE_ABS_AREA.getValue(), DEFAULT_MIN_ABS_AREA.getValue());
    var unionConf = new UnionConf((int) DEFAULT_BUFFER.getValue());
    var continuationConf = new ContinuationConf(DEFAULT_MIN_DIRECTION_THRESHOLD.getValue(), DEFAULT_MAX_DIRECTION_THRESHOLD.getValue(), DEFAULT_DISTANCE_THRESHOLD.getValue());
    var prettyConf = new PrettyConf(DEFAULT_PRETTY_CONF.getValue());
    return new RoutesContinuationConf(alphaConf, unionConf, continuationConf, prettyConf);
  }
  private static TilingConf tilingConfVal(){
      return new TilingConf(DEFAULT_Z.getValue(), DEFAULT_IMG_SIZE.getValue());
  }

  public String generatePresignedUrl(MultipartFile file) throws IOException {
    File tempInput = File.createTempFile("geojson-input-", ".geojson");
    file.transferTo(tempInput);

    var result = continueGeojson(tempInput);
    File tempOutput = File.createTempFile("geojson-output-", ".geojson");
    Files.writeString(tempOutput.toPath(), result.toString());

    String bucketKey = "geojson/result/" + tempOutput.getName();
    bucketComponent.upload(tempOutput, bucketKey);
    return bucketComponent.presign(bucketKey);
  }
}
