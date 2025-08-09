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
import app.bpartners.geojobs.repository.GeoJsonRoadContinuationRepository;
import app.bpartners.geojobs.repository.model.geojson.GeoJsonRoadContinuation;
import app.bpartners.geojobs.repository.model.geojson.RoadContinuationProcessStatus;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@AllArgsConstructor
@Slf4j
public class RoadContinuerService {
  private static final AlphaConf DEFAULT_ALPHA_CONF = new AlphaConf(0.5d, 1);
  private static final UnionConf DEFAULT_UNION_CONF = new UnionConf(1);
  private static final PrettyConf DEFAULT_PRETTY_CONF = new PrettyConf(0);
  private static final int DEFAULT_NEIGHBOUR_THRESHOLD = 10;
  private static final ContinuationConf DEFAULT_CONTINUATION_CONF =
      new ContinuationConf(PI / 12, PI / 6, 500);

  private final BucketComponent bucketComponent;
  private final GeoJsonValidator geoJsonValidator;
  private final GeoJsonRoadContinuationRepository continuationRepository;

  private static File getGeoJsonFromString(String geoJsonString) throws IOException {
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

  public static File convertMultipartFileToFile(MultipartFile multipart) throws IOException {
    String uuidName = UUID.randomUUID().toString();
    File tempFile = File.createTempFile("to-be-continued-geojson-" + uuidName, ".geojson");
    try (FileOutputStream fos = new FileOutputStream(tempFile)) {
      fos.write(multipart.getBytes());
    }
    return tempFile;
  }

  public Map<String, String> continueRoute(MultipartFile geoJSON, Integer zoom, Integer imgSize)
      throws IOException {
    if (!geoJsonValidator.isLikelyGeoJson(geoJSON))
      throw new IllegalArgumentException("Should be a geojson file");

    File geoJsonFile = convertMultipartFileToFile(geoJSON);
    var tilingConf = getTilingConf(zoom, imgSize);

    geoJsonValidator.test(geoJsonFile);
    log.info(
        "Continuing route polygons of geojson={} with zoom={} and imgSize={}",
        geoJSON.getOriginalFilename(),
        zoom,
        imgSize);

    var continuer = getLatLonContinuer(getRouteContinuationConf(), tilingConf);
    var continuedPolygons = continuer.apply(geoJsonFile);

    File continuedGeoJsonFile = getGeoJsonFromString(new Geojson(continuedPolygons).stringValue());
    log.info("Continuation process finished");

    return getPresignedURL(continuedGeoJsonFile);
  }

  private Map<String, String> getPresignedURL(File continuedGeoJsonFile) {
    var bucketKey = "continuedRoads/" + UUID.randomUUID() + ".geojson";
    bucketComponent.upload(continuedGeoJsonFile, bucketKey);
    String presignURL = bucketComponent.presign(bucketKey);
    log.info("Generated presigned URL: {}", presignURL);
    return Map.of("url", presignURL);
  }

  public TilingConf getTilingConf(Integer zoom, Integer imgSize) {
    var defaultConf = TilingConf.getDefaultInstance();
    int fZoom = (zoom == null) ? defaultConf.z() : zoom;
    int fImgSize = (imgSize == null) ? defaultConf.imgSize() : imgSize;

    return new TilingConf(fZoom, fImgSize);
  }

  @Async
  public void continueRouteAsync(
      File geoJsonFile, Integer zoom, Integer imageSize, String continuationId) {
    log.info("Starting async continuation for id={}", continuationId);

    GeoJsonRoadContinuation continuation =
        continuationRepository
            .findById(continuationId)
            .orElseThrow(
                () ->
                    new IllegalArgumentException("No continuation found for id=" + continuationId));

    Map<String, String> result;
    try {
      var tilingConf = getTilingConf(zoom, imageSize);
      geoJsonValidator.test(geoJsonFile);
      var continuer = getLatLonContinuer(getRouteContinuationConf(), tilingConf);
      var continuedPolygons = continuer.apply(geoJsonFile);
      File continuedGeoJsonFile =
          getGeoJsonFromString(new Geojson(continuedPolygons).stringValue());
      result = getPresignedURL(continuedGeoJsonFile);
    } catch (IOException e) {
      throw new RuntimeException("Error during route continuation", e);
    }

    continuation.setContinuedGeoJsonPath(result.get("url"));
    continuation.setStatus(RoadContinuationProcessStatus.CONTINUED);
    continuationRepository.save(continuation);

    log.info("Async continuation finished for id={}", continuationId);
  }
}
