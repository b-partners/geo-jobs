package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.gen.TileDetectionTaskCreated;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.repository.DetectableObjectConfigurationRepository;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.DetectionTask;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.KeyPredicateFunction;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
@Slf4j
public class DetectionTaskConsumer implements Consumer<DetectionTask> {
  private final DetectableObjectConfigurationRepository objectConfigurationRepository;
  private final KeyPredicateFunction keyPredicateFunction;
  private final EventProducer eventProducer;

  @Override
  public void accept(DetectionTask task) {
    String detectionTaskId = task.getId();
    String jobId = task.getJobId();
    if (task.getParcels().isEmpty()) {
      throw new IllegalArgumentException(
          "DetectionTask(id=" + detectionTaskId + ", jobId=" + jobId + ") does not have parcel");
    } else if (task.getParcels().stream()
        .anyMatch(parcel -> parcel.getParcelContent().getTiles().isEmpty())) {
      throw new IllegalArgumentException(
          "DetectionTask(id="
              + detectionTaskId
              + ", jobId="
              + jobId
              + ") has parcel without tiles");
    }
    var detectableObjectConf = objectConfigurationRepository.findAllByDetectionJobId(jobId);
    var detectableTypes =
        detectableObjectConf.stream().map(DetectableObjectConfiguration::getObjectType).toList();
    task.getTiles().stream()
        .filter(keyPredicateFunction.apply(Tile::getBucketPath))
        .toList()
        .forEach(
            tile -> {
              String tileDetectionTaskId = randomUUID().toString();
              String parcelId = task.getParcel().getId();
              List<TaskStatus> status =
                  List.of(
                      TaskStatus.builder()
                          .health(UNKNOWN)
                          .progression(PENDING)
                          .creationDatetime(now())
                          .taskId(tileDetectionTaskId)
                          .build());
              eventProducer.accept(
                  List.of(
                      new TileDetectionTaskCreated(
                          new TileDetectionTask(
                              tileDetectionTaskId, detectionTaskId, parcelId, jobId, tile, status),
                          detectableTypes)));
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
