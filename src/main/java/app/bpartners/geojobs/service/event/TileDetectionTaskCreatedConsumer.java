package app.bpartners.geojobs.service.event;

import static java.time.Instant.now;

import app.bpartners.geojobs.endpoint.event.gen.TileDetectionTaskCreated;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.repository.DetectedTileRepository;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.DetectedTile;
import app.bpartners.geojobs.service.detection.DetectionMapper;
import app.bpartners.geojobs.service.detection.DetectionResponse;
import app.bpartners.geojobs.service.detection.TileObjectDetector;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class TileDetectionTaskCreatedConsumer implements Consumer<TileDetectionTaskCreated> {
  private final DetectedTileRepository detectedTileRepository;
  private final TileObjectDetector objectsDetector;
  private final DetectionMapper detectionMapper;

  @Override
  public void accept(TileDetectionTaskCreated tileDetectionTaskCreated) {
    var tileDetectionTask = tileDetectionTaskCreated.getTileDetectionTask();
    var detectableTypes = tileDetectionTaskCreated.getDetectableTypes();
    DetectionResponse response = objectsDetector.apply(tileDetectionTask, detectableTypes);
    DetectedTile detectedTile =
        detectionMapper.toDetectedTile(
            response,
            tileDetectionTask.getTile(),
            tileDetectionTask.getParcelId(),
            tileDetectionTask.getJobId());
    log.info("[DEBUG] TileDetectionTaskCreatedConsumer to save tile {}", detectedTile.describe());
    var savedDetectedTile = detectedTileRepository.save(detectedTile);
    log.info(
        "[DEBUG] TileDetectionTaskCreatedConsumer saved tile {}", savedDetectedTile.describe());
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
}
