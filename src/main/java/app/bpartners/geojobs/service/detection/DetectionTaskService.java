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
  public List<MachineDetectedTile> findInDoubtTilesByJobId(
      List<MachineDetectedTile> machineDetectedTiles,
      List<DetectableObjectConfiguration> detectableObjectConfigurations) {
    return machineDetectedTiles.stream()
        .filter(
            detectedTile -> {
              if (detectedTile.getMachineDetectedObjects().isEmpty()) {
                return false;
              }
              return detectedTile.getMachineDetectedObjects().stream()
                  .anyMatch(
                      detectedObject -> detectedObject.isInDoubt(detectableObjectConfigurations));
            })
        .toList();
  }
}
