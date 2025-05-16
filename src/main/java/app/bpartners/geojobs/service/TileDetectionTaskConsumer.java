package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.*;
import static java.time.Instant.now;

import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.repository.MachineDetectedTileRepository;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.MachineDetectedTile;
import app.bpartners.geojobs.service.detection.DetectionMapper;
import app.bpartners.geojobs.service.detection.DetectionMaskCreator;
import app.bpartners.geojobs.service.detection.DetectionResponse;
import app.bpartners.geojobs.service.detection.TileObjectDetector;
import java.io.File;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class TileDetectionTaskConsumer implements TaskConsumer<TileDetectionTask> {
  private final MachineDetectedTileRepository machineDetectedTileRepository;
  private final TileObjectDetector objectsDetector;
  private final DetectionMapper detectionMapper;
  private final DetectionMaskCreator maskCreator;

  @Override
  public void accept(TileDetectionTask tileDetectionTask) {
    var detectableObjectConfigurations = tileDetectionTask.getDetectableObjectConfigurations();
    var zoneDetectionJobId = tileDetectionTask.getZoneDetectionJobId();
    var parcelJobId = tileDetectionTask.getJobId();
    var address = tileDetectionTask.getAddress();
    var point = tileDetectionTask.getPoint();
    File mask = null;
    if (isRooferModel(detectableObjectConfigurations)) {
      mask = maskCreator.createTempMask();
    }

    DetectionResponse response =
        objectsDetector.apply(tileDetectionTask, mask, detectableObjectConfigurations);
    MachineDetectedTile machineDetectedTile =
        detectionMapper.toDetectedTile(
            response,
            tileDetectionTask.getTile(),
            tileDetectionTask.getParcelId(),
            zoneDetectionJobId,
            parcelJobId);

    if (machineDetectedTile.getDetectedObjects() != null) {
      machineDetectedTile
          .getDetectedObjects()
          .forEach(
              detectedObject -> {
                if (address != null) {
                  detectedObject.getFeature().getProperties().put("address", address);
                }
                if (point != null) {
                  detectedObject.getFeature().getProperties().put("point", point);
                }
              });
    }
    log.info(
        "[DEBUG] TileDetectionTaskCreatedConsumer to save tile {}", machineDetectedTile.describe());
    machineDetectedTileRepository.save(machineDetectedTile);
  }

  public static TileDetectionTask withNewStatus(
      TileDetectionTask task,
      Status.ProgressionStatus progression,
      Status.HealthStatus health,
      String message) {
    return (TileDetectionTask)
        task.hasNewStatus(
            Status.builder()
                .progression(progression)
                .health(health)
                .creationDatetime(now())
                .message(message)
                .build());
  }

  private boolean isRooferModel(List<DetectableObjectConfiguration> configurations) {
    return configurations.stream()
        .map(DetectableObjectConfiguration::getObjectType)
        .anyMatch(
            type ->
                type.equals(TOITURE_REVETEMENT)
                    || type.name().startsWith("HUMIDITE")
                    || type.equals(OBSTACLE)
                    || type.equals(CHEMINEE)
                    || type.equals(VELUX)
                    || type.name().startsWith("USURE")
                    || type.name().startsWith("MOISISSURE")
                    || type.equals(FISSURE_CASSURE)
                    || type.name().startsWith("BATI_"));
  }
}
