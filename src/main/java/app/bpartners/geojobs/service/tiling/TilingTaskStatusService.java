package app.bpartners.geojobs.service.tiling;

import app.bpartners.geojobs.job.repository.TaskStatusRepository;
import app.bpartners.geojobs.job.service.JobService;
import app.bpartners.geojobs.job.service.TaskStatusService;
import app.bpartners.geojobs.repository.model.tiling.TilingTask;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import org.springframework.stereotype.Service;

@Service
public class TilingTaskStatusService extends TaskStatusService<TilingTask, ZoneTilingJob> {

  public TilingTaskStatusService(
      TaskStatusRepository taskStatusRepository,
      JobService<TilingTask, ZoneTilingJob> zoneJobService) {
    super(taskStatusRepository, zoneJobService);
  }
}
