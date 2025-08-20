package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.model.continuationConf.LatLonLinesContinuer.*;
import static app.bpartners.geojobs.model.continuationConf.RoutesContinuationConf.*;
import static app.bpartners.geojobs.model.continuationConf.RoutesContinuationConf.DEFAULT_DISTANCE_THRESHOLD;
import static app.bpartners.geojobs.model.continuationConf.RoutesContinuationConf.DEFAULT_MAX_DIRECTION_THRESHOLD;
import static app.bpartners.geojobs.model.continuationConf.RoutesContinuationConf.DEFAULT_MIN_DIRECTION_THRESHOLD;
import static app.bpartners.geojobs.model.continuationConf.RoutesContinuationConf.DEFAULT_PRETTY_CONF;

import app.bpartners.geojobs.endpoint.rest.postprocessing.Geojson;
import app.bpartners.geojobs.endpoint.rest.postprocessing.continuer.LatLonLinesContinuer;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TilingConf;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.model.geometry.quadrilateral.model.AlphaConf;
import app.bpartners.geojobs.model.geometry.route.ContinuationConf;
import app.bpartners.geojobs.model.geometry.route.PrettyConf;
import app.bpartners.geojobs.model.geometry.route.RoutesContinuationConf;
import app.bpartners.geojobs.model.geometry.route.UnionConf;
import app.bpartners.geojobs.repository.GeoJsonContinuationRepository;
import app.bpartners.geojobs.repository.model.geojson.GeoJsonContinuation;
import app.bpartners.geojobs.service.geojson.GeoJsonContinuerIsCompleted;
import java.io.File;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class GeoJsonContinuerRequestedService implements Consumer<GeoJsonContinuerIsCompleted> {
  private final BucketComponent bucketComponent;
  private final RoutesContinuationConf routesContinuationConf = routesContinuationConfVal();
  private final GeoJsonContinuationRepository repository;
  private final FileWriter fileWriter;

  @Override
  public void accept(GeoJsonContinuerIsCompleted geoJsonContinuerIsCompleted) {
    var id = geoJsonContinuerIsCompleted.getId();
    var fileKey = geoJsonContinuerIsCompleted.getFileKey();
    var geoJsonFile = bucketComponent.download(fileKey);
    if (repository.findById(geoJsonContinuerIsCompleted.getId()).isEmpty()) {
      log.info(
          "the geojson continuation with id{} is being treated",
          geoJsonContinuerIsCompleted.getId());
    }
    var geoJsonContinued = continueGeojson(geoJsonFile);
    uploadAndSaveToRepository(id, fileKey, geoJsonContinued);
  }

  private app.bpartners.geojobs.endpoint.rest.postprocessing.Geojson continueGeojson(
      File geoJsonToContinue) {
    var latLonLinesContinuer = getLatLonLinesContinuer();
    var latLonPolygons = latLonLinesContinuer.apply(geoJsonToContinue);
    return new Geojson(latLonPolygons);
  }

  private LatLonLinesContinuer getLatLonLinesContinuer() {
    return new LatLonLinesContinuer(
        this.routesContinuationConf,
        new TilingConf(DEFAULT_Z.getValue(), DEFAULT_IMG_SIZE.getValue()),
        DEFAULT_NEIGHBOURHOOD.getValue());
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

  private void uploadAndSaveToRepository(String id, String fileKey, Geojson geojsonContinued) {
    var geoJsonAsByte = fileWriter.writeAsByte(geojsonContinued);
    var file = fileWriter.apply(geoJsonAsByte, null);
    var geoJsonContinuation =
        new GeoJsonContinuation(id, fileKey, Status.ProgressionStatus.FINISHED);

    bucketComponent.upload(file, fileKey);
    repository.save(geoJsonContinuation);
  }
}
