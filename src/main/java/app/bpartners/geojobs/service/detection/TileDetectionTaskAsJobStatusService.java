package app.bpartners.geojobs.service.detection;

import app.bpartners.geojobs.job.repository.TaskStatusRepository;
import app.bpartners.geojobs.job.service.TaskAsJobStatusService;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.DetectionTask;
import org.springframework.stereotype.Service;

@Service
public class TileDetectionTaskAsJobStatusService
    extends TaskAsJobStatusService<TileDetectionTask, DetectionTask> {
  public TileDetectionTaskAsJobStatusService(
      TaskStatusRepository taskStatusRepository,
      TileDetectionTaskAsJobService tileDetectionTaskAsJobService) {
    super(taskStatusRepository, tileDetectionTaskAsJobService);
  }
}
