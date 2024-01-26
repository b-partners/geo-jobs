package app.bpartners.geojobs.service;

import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.ZoneTilingTaskRepository;
import app.bpartners.geojobs.repository.model.ZoneTilingTask;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class ZoneTilingTaskStatusService {

  private final ZoneTilingTaskRepository repository;
  private final ZoneTilingJobService zoneTilingJobService;
  private final ZoneTaskStatusService<ZoneTilingTask> zoneTaskStatusService;

  @Transactional(rollbackFor = IllegalArgumentException.class)
  public ZoneTilingTask pending(ZoneTilingTask task) {
    return zoneTaskStatusService.pending(task, this::update);
  }

  @Transactional(rollbackFor = IllegalArgumentException.class)
  public ZoneTilingTask process(ZoneTilingTask task) {
    return zoneTaskStatusService.process(task, this::update);
  }

  @Transactional(rollbackFor = IllegalArgumentException.class)
  public ZoneTilingTask succeed(ZoneTilingTask task) {
    return zoneTaskStatusService.succeed(task, this::update);
  }

  @Transactional(rollbackFor = IllegalArgumentException.class)
  public ZoneTilingTask fail(ZoneTilingTask task) {
    return zoneTaskStatusService.fail(task, this::update);
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
