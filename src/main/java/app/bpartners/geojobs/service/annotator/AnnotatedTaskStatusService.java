package app.bpartners.geojobs.service.annotator;

import app.bpartners.geojobs.job.repository.TaskStatusRepository;
import app.bpartners.geojobs.job.service.TaskStatusService;
import app.bpartners.geojobs.repository.model.annotator.AnnotatedTask;
import org.springframework.stereotype.Service;

@Service
public class AnnotatedTaskStatusService extends TaskStatusService<AnnotatedTask> {
  public AnnotatedTaskStatusService(TaskStatusRepository taskStatusRepository) {
    super(taskStatusRepository);
  }
}
