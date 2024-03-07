package app.bpartners.geojobs.service.detection;

import app.bpartners.geojobs.repository.DetectableObjectConfigurationRepository;
import app.bpartners.geojobs.repository.DetectedTileRepository;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.DetectedTile;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class DetectionTaskService {
  private final DetectedTileRepository detectedTileRepository;
  private final DetectableObjectConfigurationRepository objectConfigurationRepository;

  public List<DetectedTile> findInDoubtTilesByJobId(String jobId) {
    List<DetectedTile> detectedTiles = detectedTileRepository.findAllByJobId(jobId);
    List<DetectableObjectConfiguration> detectableObjectConfigurations =
        objectConfigurationRepository.findAllByDetectionJobId(jobId);
    return detectedTiles.stream()
        .filter(
            detectedTile ->
                detectedTile.getDetectedObjects().stream()
                    .anyMatch(
                        detectedObject -> detectedObject.isInDoubt(detectableObjectConfigurations)))
        .toList();
  }
}
