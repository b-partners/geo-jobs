package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.repository.model.Status.HealthStatus.FAILED;
import static app.bpartners.geojobs.repository.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.repository.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.repository.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.repository.model.Status.ProgressionStatus.PENDING;
import static app.bpartners.geojobs.repository.model.Status.ProgressionStatus.PROCESSING;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.ZoneTilingJobRepository;
import app.bpartners.geojobs.repository.ZoneTilingTaskRepository;
import app.bpartners.geojobs.repository.model.Status;
import app.bpartners.geojobs.repository.model.TilingJobStatus;
import app.bpartners.geojobs.repository.model.TilingTaskStatus;
import app.bpartners.geojobs.repository.model.ZoneTilingJob;
import app.bpartners.geojobs.repository.model.ZoneTilingTask;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ZoneTilingTaskStatusService
    extends AbstractZoneJobService<
        TilingJobStatus, ZoneTilingTask, ZoneTilingJob, ZoneTilingJobRepository> {

  private final ZoneTilingTaskRepository repository;
  private final ZoneTilingJobService zoneTilingJobService;

  public ZoneTilingTaskStatusService(
      EventProducer eventProducer,
      ZoneTilingJobRepository repository,
      ZoneTilingTaskRepository zoneTilingTaskRepository,
      ZoneTilingJobService zoneTilingJobService) {
    super(eventProducer, repository);
    this.repository = zoneTilingTaskRepository;
    this.zoneTilingJobService = zoneTilingJobService;
  }

  @Transactional(rollbackFor = IllegalArgumentException.class)
  public ZoneTilingTask pending(ZoneTilingTask task) {
    return updateStatus(task, PENDING, UNKNOWN);
  }

  @Transactional(rollbackFor = IllegalArgumentException.class)
  public ZoneTilingTask process(ZoneTilingTask task) {
    return updateStatus(task, PROCESSING, UNKNOWN);
  }

  @Transactional(rollbackFor = IllegalArgumentException.class)
  public ZoneTilingTask succeed(ZoneTilingTask task) {
    return updateStatus(task, FINISHED, SUCCEEDED);
  }

  @Transactional(rollbackFor = IllegalArgumentException.class)
  public ZoneTilingTask fail(ZoneTilingTask task) {
    return updateStatus(task, FINISHED, FAILED);
  }

  private ZoneTilingTask updateStatus(
      ZoneTilingTask task, Status.ProgressionStatus progression, Status.HealthStatus health) {
    task.addStatus(
        TilingTaskStatus.builder()
            .id(randomUUID().toString())
            .creationDatetime(now())
            .progression(progression)
            .health(health)
            .taskId(task.getId())
            .build());
    return update(task);
  }

  private ZoneTilingTask update(ZoneTilingTask zoneTilingTask) {
    if (!repository.existsById(zoneTilingTask.getId())) {
      throw new NotFoundException("ZoneTilingTask.Id = " + zoneTilingTask.getId() + " not found");
    }

    var updated = repository.save(zoneTilingTask);
    zoneTilingJobService.refreshStatus(zoneTilingTask.getJobId());

    return updated;
  }
}
