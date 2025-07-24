package app.bpartners.geojobs.service.lidar;

import app.bpartners.geojobs.repository.model.detection.DetectableType;
import app.bpartners.geojobs.service.lidar.model.LidarClass;

public class LidarClassMapper {
  public DetectableType toDomain(LidarClass classification) {
    return switch (classification) {
      case SOL -> DetectableType.BACKGROUND;
      case BATIMENT -> DetectableType.BATI_ARDOISE;
      default -> throw new IllegalStateException("Unexpected value: " + classification);
    };
  }
}
