package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.gen.DetectionTaskSucceeded;
import app.bpartners.geojobs.job.service.RetryableTaskStatusService;
import app.bpartners.geojobs.repository.DetectionTaskRepository;
import app.bpartners.geojobs.repository.model.detection.DetectionTask;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class DetectionTaskSucceededService implements Consumer<DetectionTaskSucceeded> {

  private final DetectionTaskRepository taskRepository;
  private final RetryableTaskStatusService<DetectionTask, ZoneDetectionJob> taskStatusService;

  @Override
  public void accept(DetectionTaskSucceeded tilingTaskSucceeded) {
    var task = tilingTaskSucceeded.getTask();
    taskRepository.save(task);
    taskStatusService.succeed(task);
  }
}
