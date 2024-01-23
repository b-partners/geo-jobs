package school.hei.geojobs.service;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static school.hei.geojobs.repository.model.Status.HealthStatus.FAILED;
import static school.hei.geojobs.repository.model.Status.HealthStatus.SUCCEEDED;
import static school.hei.geojobs.repository.model.Status.HealthStatus.UNKNOWN;
import static school.hei.geojobs.repository.model.Status.ProgressionStatus.FINISHED;
import static school.hei.geojobs.repository.model.Status.ProgressionStatus.PROCESSING;

import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import school.hei.geojobs.endpoint.event.EventProducer;
import school.hei.geojobs.model.exception.NotFoundException;
import school.hei.geojobs.repository.ZoneTilingJobRepository;
import school.hei.geojobs.repository.ZoneTilingTaskRepository;
import school.hei.geojobs.repository.model.Status;
import school.hei.geojobs.repository.model.Status.HealthStatus;
import school.hei.geojobs.repository.model.Status.ProgressionStatus;
import school.hei.geojobs.repository.model.TilingJobStatus;
import school.hei.geojobs.repository.model.TilingTaskStatus;
import school.hei.geojobs.repository.model.ZoneTilingJob;
import school.hei.geojobs.repository.model.ZoneTilingTask;

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
      ZoneTilingTask task, ProgressionStatus progression, HealthStatus health) {
    var job = findById(task.getJobId());
    Status.reduce(
        job.getTasks().stream()
            .map(ZoneTilingTask::getStatus)
            .map(status -> (Status) status)
            .collect(Collectors.toUnmodifiableList()));
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
