package app.bpartners.geojobs.utils.detection;

import static app.bpartners.geojobs.service.event.ZoneDetectionJobSucceededService.DEFAULT_MINIMUM_CONFIDENCE_FOR_DELIVERY;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.utils.FeatureCreator;
import java.util.List;

public class DetectionCreator {
  private final FeatureCreator featureCreator;

  public DetectionCreator(FeatureCreator featureCreator) {
    this.featureCreator = featureCreator;
  }

  public Detection createFromZTJAndZDJ(String tilingJobId, String detectionJobId) {
    var detectionId = randomUUID().toString();
    return create(detectionId, tilingJobId, detectionJobId);
  }

  public Detection create(String detectionId, String tilingJobId, String detectionJobId) {
    return create(detectionId, tilingJobId, detectionJobId, featureCreator.defaultFeatures())
        .toBuilder()
        .endToEndId(randomUUID().toString())
        .build();
  }

  public Detection create(
      String detectionId, String tilingJobId, String detectionJobId, List<Feature> geoJson) {
    return Detection.builder()
        .id(detectionId)
        .endToEndId(detectionId)
        .ztjId(tilingJobId)
        .zdjId(detectionJobId)
        .geojsonS3FileKey(null)
        .detectableObjectConfigurations(
            List.of(
                app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration
                    .builder()
                    .bucketStorageName(null)
                    .objectType(DetectableType.TOITURE_REVETEMENT)
                    .minConfidenceForDetection(
                        DEFAULT_MINIMUM_CONFIDENCE_FOR_DELIVERY) // TODO do not confuse
                    .build()))
        .geoJsonZone(geoJson)
        .build();
  }
}
