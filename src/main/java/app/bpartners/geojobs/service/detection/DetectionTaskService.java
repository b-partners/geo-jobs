package app.bpartners.geojobs.service.detection;

import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.DetectedTile;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class DetectionTaskService {

  @Transactional
  public List<DetectedTile> findInDoubtTilesByJobId(
      List<DetectedTile> detectedTiles,
      List<DetectableObjectConfiguration> detectableObjectConfigurations) {
    return detectedTiles.stream()
        .filter(
            detectedTile -> {
              if (detectedTile.getDetectedObjects().isEmpty()) {
                return false;
              }
              return detectedTile.getDetectedObjects().stream()
                  .anyMatch(
                      detectedObject -> detectedObject.isInDoubt(detectableObjectConfigurations));
            })
        .toList();
  }
}
