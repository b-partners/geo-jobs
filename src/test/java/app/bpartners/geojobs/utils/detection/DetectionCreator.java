package app.bpartners.geojobs.utils.detection;

import static app.bpartners.geojobs.service.event.ZoneDetectionFinishedConsumer.DEFAULT_MIN_CONFIDENCE;
import static java.util.UUID.randomUUID;

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
                    .confidence(DEFAULT_MIN_CONFIDENCE)
                    .build()))
        .geoJsonZone(featureCreator.defaultFeatures())
        .build();
  }
}
