package app.bpartners.geojobs.service.detection;

import app.bpartners.geojobs.job.repository.TaskStatusRepository;
import app.bpartners.geojobs.job.service.JobService;
import app.bpartners.geojobs.job.service.TaskStatusService;
import app.bpartners.geojobs.repository.model.detection.DetectionTask;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import org.springframework.stereotype.Service;

@Service
public class DetectionTaskStatusService extends TaskStatusService<DetectionTask, ZoneDetectionJob> {

  public DetectionTaskStatusService(
      TaskStatusRepository taskStatusRepository,
      JobService<DetectionTask, ZoneDetectionJob> zoneJobService) {
    super(taskStatusRepository, zoneJobService);
  }
}
