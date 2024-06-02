package app.bpartners.geojobs.service.detection;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.DetectionTaskStatusChanged;
import app.bpartners.geojobs.job.repository.TaskRepository;
import app.bpartners.geojobs.job.repository.TaskStatusRepository;
import app.bpartners.geojobs.job.service.TaskToTaskService;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.DetectionTask;
import java.util.List;
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

  @Override
  protected void onStatusChanged(DetectionTask oldTask, DetectionTask newTask) {
    eventProducer.accept(List.of(new DetectionTaskStatusChanged(oldTask, newTask)));
  }
}
