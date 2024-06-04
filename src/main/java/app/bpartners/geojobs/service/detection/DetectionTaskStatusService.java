package app.bpartners.geojobs.service.detection;

import app.bpartners.geojobs.job.repository.TaskStatusRepository;
import app.bpartners.geojobs.job.service.TaskStatusService;
import app.bpartners.geojobs.repository.model.detection.DetectionTask;
import org.springframework.stereotype.Service;

@Service
public class DetectionTaskStatusService extends TaskStatusService<DetectionTask> {

  public DetectionTaskStatusService(TaskStatusRepository taskStatusRepository) {
    super(taskStatusRepository);
  }
}
