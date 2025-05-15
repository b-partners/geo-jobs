package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.model.ModelName.TOITURE;

import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.Point;
import app.bpartners.geojobs.endpoint.rest.validator.FeatureTypeChecker;
import app.bpartners.geojobs.file.bucket.BucketComponent;
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
  private final FeatureTypeChecker featureTypeChecker;

  @Override
  public List<Feature> apply(Detection detection) {
    var providedGeoJsonZone = detection.getProvidedGeoJsonZone();
    if (providedGeoJsonZone == null) {
      return null;
    }
    var detectableObjectModel = detection.getDetectableObjectModel();
    if (detectableObjectModel == null) {
      return providedGeoJsonZone;
    }
    var featuresHasAllPoints = featureTypeChecker.apply(providedGeoJsonZone, Point.class);
    var toitureDetection = TOITURE.equals(detectableObjectModel.getModelName());
    if (toitureDetection && featuresHasAllPoints) {
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
          var point = feature.getGeometry().getPoint();
          var longitude = point.getCoordinates().getFirst();
          var latitude = point.getCoordinates().getLast();

          var fileKey = layer + "/extended_original_" + longitude + "_" + latitude + ".jpg";
          try {
            var originalImageUrl = bucketComponent.presign(fileKey, Duration.ofHours(1L));
            var properties =
                feature.getProperties() == null
                    ? new HashMap<String, Object>()
                    : feature.getProperties();
            properties.put("original_image_url", originalImageUrl);
            feature.setProperties(properties);
          } catch (RuntimeException ignored) {
            log.warn(
                "Unable to presign file {}, exception caught {}", fileKey, ignored.getMessage());
          }
        });
    return updatedGeoJson;
  }
}
