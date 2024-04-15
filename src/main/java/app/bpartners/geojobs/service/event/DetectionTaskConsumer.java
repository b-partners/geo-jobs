package app.bpartners.geojobs.service.event;

import static java.time.Instant.now;

import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.repository.DetectableObjectConfigurationRepository;
import app.bpartners.geojobs.repository.DetectedTileRepository;
import app.bpartners.geojobs.repository.model.TileTask;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.DetectedTile;
import app.bpartners.geojobs.repository.model.detection.DetectionTask;
import app.bpartners.geojobs.service.detection.DetectionMapper;
import app.bpartners.geojobs.service.detection.DetectionResponse;
import app.bpartners.geojobs.service.detection.TileObjectDetector;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
@Slf4j
public class DetectionTaskConsumer implements Consumer<DetectionTask> {
  private final DetectedTileRepository detectedTileRepository;
  private final TileObjectDetector objectsDetector;
  private final DetectableObjectConfigurationRepository objectConfigurationRepository;
  private final DetectionMapper detectionMapper;

  @Override
  public void accept(DetectionTask task) {
    var detectedParcel = task.getParcel();
    var associatedTile = task.getTile();
    String taskId = task.getId();
    String jobId = task.getJobId();
    if (detectedParcel == null || associatedTile == null) {
      throw new IllegalArgumentException(
          "DetectionTask(id=" + taskId + ", jobId=" + jobId + ") does not have parcel or tile");
    }
    var detectableObjectConf = objectConfigurationRepository.findAllByDetectionJobId(jobId);
    var detectableTypes =
        detectableObjectConf.stream().map(DetectableObjectConfiguration::getObjectType).toList();
    task.getTiles()
        .forEach(
            tile -> {
              DetectionResponse response =
                  objectsDetector.apply(new TileTask(taskId, jobId, tile), detectableTypes);
              DetectedTile detectedTile =
                  detectionMapper.toDetectedTile(
                      response, associatedTile, detectedParcel.getId(), jobId);
              log.error("[DEBUG] DetectionTaskConsumer to save tile {}", detectedTile.describe());
              var savedDetectedTile = detectedTileRepository.save(detectedTile);
              log.error(
                  "[DEBUG] DetectionTaskConsumer saved tile {}", savedDetectedTile.describe());
            });
  }

  public static DetectionTask withNewStatus(
      DetectionTask task,
      Status.ProgressionStatus progression,
      Status.HealthStatus health,
      String message) {
    return (DetectionTask)
        task.hasNewStatus(
            Status.builder()
                .progression(progression)
                .health(health)
                .creationDatetime(now())
                .message(message)
                .build());
  }
}
