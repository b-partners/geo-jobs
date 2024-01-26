package app.bpartners.geojobs.service;

import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.ZoneDetectionTaskRepository;
import app.bpartners.geojobs.repository.model.ZoneDetectionTask;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ZoneDetectionTaskStatusService {
  private final ZoneDetectionTaskRepository repository;
  private final ZoneDetectionJobService zoneDetectionJobService;
  private final ZoneTaskStatusService<ZoneDetectionTask> zoneTaskStatusService;

  public ZoneDetectionTask process(ZoneDetectionTask task) {
    return zoneTaskStatusService.process(task, this::update);
  }

  public ZoneDetectionTask succeed(ZoneDetectionTask task) {
    return zoneTaskStatusService.succeed(task, this::update);
  }

  public ZoneDetectionTask fail(ZoneDetectionTask task) {
    return zoneTaskStatusService.fail(task, this::update);
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
