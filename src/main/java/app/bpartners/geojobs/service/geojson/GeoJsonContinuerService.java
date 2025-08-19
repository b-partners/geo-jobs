package app.bpartners.geojobs.service.geojson;

import static app.bpartners.geojobs.model.continuationConf.LatLonLinesContinuer.*;
import static app.bpartners.geojobs.model.continuationConf.RoutesContinuationConf.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.rest.postprocessing.Geojson;
import app.bpartners.geojobs.endpoint.rest.postprocessing.continuer.LatLonLinesContinuer;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.LatLonPolygon;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TilingConf;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.model.geometry.quadrilateral.model.AlphaConf;
import app.bpartners.geojobs.model.geometry.route.ContinuationConf;
import app.bpartners.geojobs.model.geometry.route.PrettyConf;
import app.bpartners.geojobs.model.geometry.route.RoutesContinuationConf;
import app.bpartners.geojobs.model.geometry.route.UnionConf;
import java.io.File;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class GeoJsonContinuerService {
  private final BucketComponent bucketComponent;
  private final FileWriter fileWriter;
  private final EventProducer<GeoJsonContinuerIsCompleted> eventProducer;

  private final RoutesContinuationConf routesContinuationConf = routesContinuationConfVal();

  public Geojson continueGeojson(File geoJsonToContinue) {
    var latLonLinesContinuer = getLatLonLinesContinuer();
    Set<LatLonPolygon> features = latLonLinesContinuer.apply(geoJsonToContinue);
    return new Geojson(features);
  }

  private static RoutesContinuationConf routesContinuationConfVal() {
    var alphaConf =
        new AlphaConf(DEFAULT_MIN_COVERAGE_ABS_AREA.getValue(), DEFAULT_MIN_ABS_AREA.getValue());
    var unionConf = new UnionConf((int) DEFAULT_BUFFER.getValue());
    var continuationConf =
        new ContinuationConf(
            DEFAULT_MIN_DIRECTION_THRESHOLD.getValue(),
            DEFAULT_MAX_DIRECTION_THRESHOLD.getValue(),
            DEFAULT_DISTANCE_THRESHOLD.getValue());
    var prettyConf = new PrettyConf(DEFAULT_PRETTY_CONF.getValue());
    return new RoutesContinuationConf(alphaConf, unionConf, continuationConf, prettyConf);
  }

  public String generatePresignedUrl(File file) {
    var fileBytes = fileWriter.writeAsByte(file);
    var tempDir = FileWriter.createTempDirectory();
    var tempInput = fileWriter.apply(fileBytes, tempDir);
    var result = continueGeojson(tempInput);
    eventProducer.accept(List.of(GeoJsonContinuerIsCompleted.builder().geoJson(result).build()));

    var resultBytes = result.toString().getBytes();
    var tempOutput = fileWriter.write(resultBytes, tempDir, "geojson-output");

    var bucketKey = "geojson/result/" + tempOutput.getName();
    bucketComponent.upload(tempOutput, bucketKey);
    return bucketComponent.presign(bucketKey);
  }

  private LatLonLinesContinuer getLatLonLinesContinuer() {
    return new LatLonLinesContinuer(
        this.routesContinuationConf,
        new TilingConf(DEFAULT_Z.getValue(), DEFAULT_IMG_SIZE.getValue()),
        DEFAULT_NEIGHBOURHOOD.getValue());
  }
}
