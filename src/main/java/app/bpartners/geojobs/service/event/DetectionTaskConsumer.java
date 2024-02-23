package app.bpartners.geojobs.service.event;

import static java.time.Instant.now;

import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.repository.DetectedTileRepository;
import app.bpartners.geojobs.repository.model.detection.DetectedTile;
import app.bpartners.geojobs.repository.model.detection.DetectionTask;
import app.bpartners.geojobs.service.detection.DetectionMapper;
import app.bpartners.geojobs.service.detection.DetectionResponse;
import app.bpartners.geojobs.service.detection.ObjectsDetector;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class DetectionTaskConsumer implements Consumer<DetectionTask> {
  private final DetectedTileRepository detectedTileRepository;
  private final ObjectsDetector objectsDetector;

  @Override
  public void accept(DetectionTask task) {
    DetectionResponse response = objectsDetector.apply(task);
    DetectedTile detectedTile =
        DetectionMapper.toDetectedTile(response, task.getTile(), task.getJobId());
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
