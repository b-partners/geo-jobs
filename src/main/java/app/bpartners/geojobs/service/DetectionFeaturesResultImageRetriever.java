package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.model.ModelName.TOITURE;

import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.Point;
import app.bpartners.geojobs.endpoint.rest.validator.FeatureTypeChecker;
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
  private final FeatureTypeChecker featureTypeChecker;
  private final CustomBucketComponent customBucketComponent;

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

          var originalFileKey = layer + "/extended_original_" + longitude + "_" + latitude + ".jpg";
          var drawnFileKey = layer + "/extended_drawn_" + longitude + "_" + latitude + ".jpg";

          addImageIfExist(originalFileKey, feature, "original_image_url");

          addImageIfExist(drawnFileKey, feature, "drawn_image_url");
        });
    return updatedGeoJson;
  }

  private void addImageIfExist(String fileKey, Feature feature, String fileProperty) {
    var fileExist =
        customBucketComponent.listObjects(bucketComponent.getBucketName(), fileKey).stream()
            .findAny()
            .isPresent();
    if (fileExist) {
      try {
        var imageUrl = bucketComponent.presign(fileKey, Duration.ofHours(1L));
        var properties =
            feature.getProperties() == null
                ? new HashMap<String, Object>()
                : feature.getProperties();
        properties.put(fileProperty, imageUrl);
        feature.setProperties(properties);
      } catch (RuntimeException ignored) {
        log.warn("Unable to presign file {}, exception caught {}", fileKey, ignored.getMessage());
      }
    }
  }
}
