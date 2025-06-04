package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper.getCentroidRestPointFromPolygon;

import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.endpoint.rest.model.Point;
import app.bpartners.geojobs.endpoint.rest.model.Polygon;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.file.bucket.CustomBucketComponent;
import app.bpartners.geojobs.repository.model.detection.Detection;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DetectionFeaturesResultImageRetriever implements Function<Detection, List<Feature>> {
  private final BucketComponent bucketComponent;
  private final CustomBucketComponent customBucketComponent;

  @Override
  public List<Feature> apply(Detection detection) {
    var providedGeoJsonZone = detection.getProvidedGeoJsonZone();
    if (providedGeoJsonZone == null) {
      return null;
    }
    if (!detection.isSucceeded() && !detection.isSynchronous()) {
      return providedGeoJsonZone;
    }
    var detectableObjectModel = detection.getDetectableObjectModel();
    if (detectableObjectModel == null) {
      return providedGeoJsonZone;
    }

    if (detection.hasToitureModelName()) {
      var layer = detection.getGeoServerProperties().getGeoServerParameter().getLayers();
      if (layer == null) {
        return providedGeoJsonZone;
      }
      return retrieveFeatureImageFromBucket(providedGeoJsonZone, layer);
    }
    return providedGeoJsonZone;
  }

  private ArrayList<Feature> retrieveFeatureImageFromBucket(
      List<Feature> providedGeoJsonZone, String layer) {
    var updatedGeoJson = new ArrayList<>(providedGeoJsonZone);
    updatedGeoJson.forEach(
        feature -> {
          var geometryType = feature.getGeometry().getActualInstance();
          Point point;
          switch (geometryType) {
            case Point ignored -> point = feature.getGeometry().getPoint();
            case Polygon ignored -> point = getCentroidRestPointFromPolygon(feature);
            case MultiPolygon ignored -> point = getCentroidRestPointFromPolygon(feature);
            default ->
                throw new IllegalArgumentException("Unsupported geometry type: " + geometryType);
          }
          if (point != null) {
            var longitude = point.getCoordinates().getFirst();
            var latitude = point.getCoordinates().getLast();
            var originalFileKey =
                layer + "/extended_original_" + longitude + "_" + latitude + ".jpg";
            var drawnFileKey = layer + "/extended_drawn_" + longitude + "_" + latitude + ".jpg";
            var vggFileKey = layer + "/vgg_" + longitude + "_" + latitude + ".json";

            addPropertyIfFileKeyExist(originalFileKey, feature, "original_image_url");

            addPropertyIfFileKeyExist(drawnFileKey, feature, "drawn_image_url");

            addPropertyIfFileKeyExist(vggFileKey, feature, "vgg_file_url");
          }
        });
    return updatedGeoJson;
  }

  private void addPropertyIfFileKeyExist(String fileKey, Feature feature, String fileProperty) {
    var fileExist =
        customBucketComponent.listObjects(bucketComponent.getBucketName(), fileKey).stream()
            .findAny()
            .isPresent();
    if (fileExist) {
      try {
        var propertyUrl = bucketComponent.presign(fileKey, Duration.ofHours(1L));
        var properties =
            feature.getProperties() == null
                ? new HashMap<String, Object>()
                : feature.getProperties();
        properties.put(fileProperty, propertyUrl);
        feature.setProperties(properties);
      } catch (RuntimeException ignored) {
        log.warn("Unable to presign file {}, exception caught {}", fileKey, ignored.getMessage());
      }
    }
  }
}
