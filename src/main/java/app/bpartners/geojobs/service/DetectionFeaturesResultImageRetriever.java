package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper.getCentroidRestPointFromPolygon;

import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.endpoint.rest.model.Point;
import app.bpartners.geojobs.endpoint.rest.model.Polygon;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.file.bucket.CustomBucketComponent;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
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
  private final GeometryConverter geometryConverter;

  @Override
  public List<Feature> apply(Detection detection) {
    var providedGeoJsonZone = detection.getProvidedGeoJsonZone();
    var splitPolygonGeoJsonZone = detection.getSplitPolygonGeoJsonZone();
    if (providedGeoJsonZone == null && splitPolygonGeoJsonZone == null) {
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
      return retrieveFeatureImageFromBucket(
          providedGeoJsonZone, splitPolygonGeoJsonZone, layer, detection.needsImageOutput());
    }
    return providedGeoJsonZone;
  }

  private ArrayList<Feature> retrieveFeatureImageFromBucket(
      List<Feature> providedGeoJsonZone,
      List<Feature> splitPolygonGeoJsonZone,
      String layer,
      boolean detectionNeedsImageOutput) {
    ArrayList<Feature> updatedGeoJson;
    if (splitPolygonGeoJsonZone != null
        && !splitPolygonGeoJsonZone.isEmpty()
        && detectionNeedsImageOutput) {
      updatedGeoJson = new ArrayList<>(splitPolygonGeoJsonZone);
      var providedFeature = providedGeoJsonZone.getFirst();
      if (providedFeature.getProperties() == null) {
        HashMap<String, Object> properties = new HashMap<>();
        properties.put("description", "Original zone provided");
        providedFeature.setProperties(properties);
      } else {
        providedFeature.getProperties().put("description", "Original zone provided");
      }
      updatedGeoJson.addFirst(providedFeature);
    } else {
      updatedGeoJson = new ArrayList<>(providedGeoJsonZone);
    }

    updatedGeoJson.forEach(
        feature -> {
          var geometryType = feature.getGeometry().getActualInstance();
          Point centroidFromProperties;
          switch (geometryType) {
            case Point point -> centroidFromProperties = point;
            case Polygon ignored ->
                centroidFromProperties = getCentroidRestPointFromPolygon(feature);
            case MultiPolygon ignored ->
                centroidFromProperties = getCentroidRestPointFromPolygon(feature);
            default ->
                throw new IllegalArgumentException(
                    "Unsupported geometry type to retrieve centroid from properties : "
                        + geometryType);
          }
          var centroidCoordinates =
              centroidFromProperties == null
                  ? geometryConverter.centroidFromGeometry(geometryType)
                  : centroidFromProperties.getCoordinates();
          var longitude = centroidCoordinates.getFirst();
          var latitude = centroidCoordinates.getLast();
          var originalFileKey = layer + "/extended_original_" + longitude + "_" + latitude + ".jpg";
          var drawnFileKey = layer + "/extended_drawn_" + longitude + "_" + latitude + ".jpg";
          var vggFileKey = layer + "/vgg_" + longitude + "_" + latitude + ".json";

          addPropertyIfFileKeyExist(originalFileKey, feature, "original_image_url");

          addPropertyIfFileKeyExist(drawnFileKey, feature, "drawn_image_url");

          addPropertyIfFileKeyExist(vggFileKey, feature, "vgg_file_url");
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
