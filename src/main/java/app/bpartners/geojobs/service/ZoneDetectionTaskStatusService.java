package app.bpartners.geojobs.service;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.ZoneDetectionTaskRepository;
import app.bpartners.geojobs.repository.model.Status;
import app.bpartners.geojobs.repository.model.TaskStatus;
import app.bpartners.geojobs.repository.model.ZoneDetectionTask;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ZoneDetectionTaskStatusService {
  private final ZoneDetectionTaskRepository repository;
  private final ZoneDetectionJobService zoneDetectionJobService;

  public ZoneDetectionTask process(ZoneDetectionTask task) {
    return updateStatus(task, Status.ProgressionStatus.PROCESSING, Status.HealthStatus.UNKNOWN);
  }

  public ZoneDetectionTask succeed(ZoneDetectionTask task) {
    return updateStatus(task, Status.ProgressionStatus.FINISHED, Status.HealthStatus.SUCCEEDED);
  }

  public ZoneDetectionTask fail(ZoneDetectionTask task) {
    return updateStatus(task, Status.ProgressionStatus.FINISHED, Status.HealthStatus.FAILED);
  }

  private ZoneDetectionTask updateStatus(
      ZoneDetectionTask task, Status.ProgressionStatus progression, Status.HealthStatus health) {
    task.addStatus(
        TaskStatus.builder()
            .id(randomUUID().toString())
            .creationDatetime(now())
            .progression(progression)
            .health(health)
            .taskId(task.getId())
            .build());
    return update(task);
  }

  private ZoneDetectionTask update(ZoneDetectionTask zoneDetectionTask) {
    if (!repository.existsById(zoneDetectionTask.getId())) {
      throw new NotFoundException("ZoneDetection.Id = " + zoneDetectionTask.getId() + " not found");
    }

    var updated = repository.save(zoneDetectionTask);
    zoneDetectionJobService.refreshStatus(zoneDetectionTask.getJobId());

    return updated;
  }
}
