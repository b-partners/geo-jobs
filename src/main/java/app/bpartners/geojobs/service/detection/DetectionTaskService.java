package app.bpartners.geojobs.service.detection;

import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.MachineDetectedTile;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class DetectionTaskService {

  @Transactional
  public List<MachineDetectedTile> filterByInDoubt(
      List<MachineDetectedTile> machineDetectedTiles,
      List<DetectableObjectConfiguration> detectableObjectConfigurations) {
    return machineDetectedTiles.stream()
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

  public List<MachineDetectedTile> filterByConfidence(
      Double confidence, List<MachineDetectedTile> detectedTiles, boolean isGreaterThan) {
    return detectedTiles.stream()
        .filter(
            detectedTile -> {
              if (detectedTile.getDetectedObjects().isEmpty()) {
                return false;
              }
              return isGreaterThan
                  ? detectedTile.getDetectedObjects().stream()
                      .anyMatch(tile -> tile.getComputedConfidence() >= confidence)
                  : detectedTile.getDetectedObjects().stream()
                      .anyMatch(tile -> tile.getComputedConfidence() < confidence);
            })
        .toList();
  }
}
