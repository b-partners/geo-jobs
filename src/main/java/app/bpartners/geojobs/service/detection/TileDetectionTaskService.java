package app.bpartners.geojobs.service.detection;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.job.repository.TaskRepository;
import app.bpartners.geojobs.job.repository.TaskStatusRepository;
import app.bpartners.geojobs.job.service.TaskToTaskService;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.DetectionTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
public class TileDetectionTaskService extends TaskToTaskService<TileDetectionTask, DetectionTask> {
  protected TileDetectionTaskService(
      JpaRepository<DetectionTask, String> parentRepository,
      TaskStatusRepository taskStatusRepository,
      TaskRepository<TileDetectionTask> tileDetectionTaskTaskRepository,
      EventProducer eventProducer) {
    super(
        parentRepository,
        taskStatusRepository,
        tileDetectionTaskTaskRepository,
        eventProducer,
        DetectionTask.class);
  }
}
