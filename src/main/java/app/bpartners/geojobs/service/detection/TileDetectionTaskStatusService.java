package app.bpartners.geojobs.service.detection;

import app.bpartners.geojobs.job.repository.TaskStatusRepository;
import app.bpartners.geojobs.job.service.TaskToTaskStatusService;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.DetectionTask;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import org.springframework.stereotype.Service;

@Service
public class TileDetectionTaskStatusService
    extends TaskToTaskStatusService<TileDetectionTask, DetectionTask, ZoneDetectionJob> {
  public TileDetectionTaskStatusService(
      DetectionTaskStatusService detectionTaskStatusService,
      TaskStatusRepository taskStatusRepository,
      TileDetectionTaskService taskToTaskService) {
    super(detectionTaskStatusService, taskStatusRepository, taskToTaskService);
  }
}
