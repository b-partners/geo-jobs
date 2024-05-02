package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.gen.TileDetectionTaskSucceeded;
import app.bpartners.geojobs.job.service.RetryableTaskToTaskStatusService;
import app.bpartners.geojobs.repository.TileDetectionTaskRepository;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.DetectionTask;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class TileDetectionTaskSucceededService implements Consumer<TileDetectionTaskSucceeded> {
  private final RetryableTaskToTaskStatusService<TileDetectionTask, DetectionTask, ZoneDetectionJob>
      taskToTaskStatusService;
  private final TileDetectionTaskRepository tileDetectionTaskRepository;

  @Override
  public void accept(TileDetectionTaskSucceeded tileDetectionTaskSucceeded) {
    var tileDetectionTask = tileDetectionTaskSucceeded.getTileDetectionTask();
    tileDetectionTaskRepository.save(tileDetectionTask);
    taskToTaskStatusService.succeed(tileDetectionTask);
  }
}
