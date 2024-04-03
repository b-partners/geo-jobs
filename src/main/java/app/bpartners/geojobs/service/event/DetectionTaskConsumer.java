package app.bpartners.geojobs.service.event;

import static java.time.Instant.now;

import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.repository.DetectableObjectConfigurationRepository;
import app.bpartners.geojobs.repository.DetectedTileRepository;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.DetectedTile;
import app.bpartners.geojobs.repository.model.detection.DetectionTask;
import app.bpartners.geojobs.service.detection.DetectionMapper;
import app.bpartners.geojobs.service.detection.DetectionResponse;
import app.bpartners.geojobs.service.detection.ObjectsDetector;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
@Slf4j
public class DetectionTaskConsumer implements Consumer<DetectionTask> {
  private final DetectedTileRepository detectedTileRepository;
  private final ObjectsDetector objectsDetector;
  private final DetectableObjectConfigurationRepository objectConfigurationRepository;

  @Override
  public void accept(DetectionTask task) {
    var detectedParcel = task.getParcel();
    var associatedTile = task.getTile();
    if (detectedParcel == null || associatedTile == null) {
      throw new IllegalArgumentException(
          "DetectionTask(id="
              + task.getId()
              + ", jobId="
              + task.getJobId()
              + ") does not have parcel or tile");
    }
    var detectableObjectConf =
        objectConfigurationRepository.findAllByDetectionJobId(task.getJobId());
    var detectableTypes =
        detectableObjectConf.stream().map(DetectableObjectConfiguration::getObjectType).toList();
    DetectionResponse response = objectsDetector.apply(task, detectableTypes);
    DetectedTile detectedTile =
        DetectionMapper.toDetectedTile(
            response, associatedTile, detectedParcel.getId(), task.getJobId());
    detectedTileRepository.save(detectedTile);
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
