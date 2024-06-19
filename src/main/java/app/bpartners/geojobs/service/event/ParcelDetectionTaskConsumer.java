package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.*;
import static java.time.Instant.now;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.ParcelDetectionJobCreated;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.repository.ParcelDetectionTaskRepository;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionJob;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionTask;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.KeyPredicateFunction;
import app.bpartners.geojobs.service.TaskToJobConverter;
import app.bpartners.geojobs.service.detection.ParcelDetectionJobService;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@AllArgsConstructor
@Component
@Slf4j
public class ParcelDetectionTaskConsumer implements Consumer<ParcelDetectionTask> {
  private final EventProducer eventProducer;
  private final TaskToJobConverter<ParcelDetectionTask, ParcelDetectionJob> taskToJobConverter;
  private final ParcelDetectionJobService parcelDetectionJobService;
  private final KeyPredicateFunction keyPredicateFunction;
  private final ParcelDetectionTaskRepository parcelDetectionTaskRepository;

  @Override
  @Transactional
  public void accept(ParcelDetectionTask task) {
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
    ParcelDetectionJob parcelDetectionJob = taskToJobConverter.apply(task);
    task.setAsJobId(parcelDetectionJob.getId());
    ParcelDetectionTask savedParcelTask = parcelDetectionTaskRepository.save(task);
    List<TileDetectionTask> tileDetectionTasks =
        savedParcelTask.getTiles().stream()
            .filter(keyPredicateFunction.apply(Tile::getBucketPath))
            .map(tile -> taskToJobConverter.apply(savedParcelTask, tile))
            .toList();
    ParcelDetectionJob createdParcelDetectionJob =
        parcelDetectionJobService.create(parcelDetectionJob, tileDetectionTasks);
    eventProducer.accept(
        List.of(
            ParcelDetectionJobCreated.builder()
                .parcelDetectionJob(createdParcelDetectionJob)
                .build()));
  }

  public static ParcelDetectionTask withNewStatus(
      ParcelDetectionTask task,
      Status.ProgressionStatus progression,
      Status.HealthStatus health,
      String message) {
    return (ParcelDetectionTask)
        task.hasNewStatus(
            Status.builder()
                .progression(progression)
                .health(health)
                .creationDatetime(now())
                .message(message)
                .build());
  }
}
