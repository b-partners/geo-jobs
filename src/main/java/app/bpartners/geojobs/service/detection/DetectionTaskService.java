package app.bpartners.geojobs.service.detection;

import app.bpartners.geojobs.repository.DetectableObjectConfigurationRepository;
import app.bpartners.geojobs.repository.DetectedTileRepository;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.DetectedTile;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Slf4j
public class DetectionTaskService {
  private final DetectedTileRepository detectedTileRepository;
  private final DetectableObjectConfigurationRepository objectConfigurationRepository;

  @Transactional
  public List<DetectedTile> findInDoubtTilesByJobId(
      String jobId, List<DetectedTile> detectedTiles) {
    List<DetectableObjectConfiguration> detectableObjectConfigurations =
        objectConfigurationRepository.findAllByDetectionJobId(jobId);
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
