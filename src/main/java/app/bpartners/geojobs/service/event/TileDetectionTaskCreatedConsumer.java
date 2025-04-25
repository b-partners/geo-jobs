package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.*;
import static java.time.Instant.now;

import app.bpartners.geojobs.endpoint.event.model.tile.TileDetectionTaskCreated;
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
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class TileDetectionTaskCreatedConsumer implements Consumer<TileDetectionTaskCreated> {
  private final MachineDetectedTileRepository machineDetectedTileRepository;
  private final TileObjectDetector objectsDetector;
  private final DetectionMapper detectionMapper;
  private final DetectionMaskCreator maskCreator;

  @Override
  public void accept(TileDetectionTaskCreated tileDetectionTaskCreated) {
    var tileDetectionTask = tileDetectionTaskCreated.getTileDetectionTask();
    var detectableObjectConfigurations =
        tileDetectionTaskCreated.getDetectableObjectConfigurations();
    var zoneDetectionJobId = tileDetectionTaskCreated.getZoneDetectionJobId();
    var parcelJobId = tileDetectionTask.getJobId();
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
                    || type.equals(HUMIDITE)
                    || type.equals(OBSTACLE)
                    || type.equals(CHEMINEE)
                    || type.equals(VELUX)
                    || type.equals(USURE)
                    || type.equals(FISSURE_CASSURE)
                    || type.name().startsWith("BATI_"));
  }
}
