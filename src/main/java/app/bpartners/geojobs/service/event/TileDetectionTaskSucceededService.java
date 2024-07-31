package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.TileDetectionTaskSucceeded;
import app.bpartners.geojobs.endpoint.event.model.ZDJStatusRecomputingSubmitted;
import app.bpartners.geojobs.repository.TileDetectionTaskRepository;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.service.detection.TileDetectionTaskStatusService;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class TileDetectionTaskSucceededService implements Consumer<TileDetectionTaskSucceeded> {
  private final TileDetectionTaskStatusService tileDetectionTaskStatusService;
  private final TileDetectionTaskRepository tileDetectionTaskRepository;
  private final EventProducer eventProducer;

  @Override
  public void accept(TileDetectionTaskSucceeded tileDetectionTaskSucceeded) {
    var tileDetectionTask = tileDetectionTaskSucceeded.getTileDetectionTask();
    tileDetectionTaskRepository.save(tileDetectionTask);
    tileDetectionTaskStatusService.succeed(tileDetectionTask);
    List<TileDetectionTask> tasks =
        tileDetectionTaskRepository.findAllByJobId(tileDetectionTask.getJobId());
    if (isFinished(tasks)) {
      eventProducer.accept(
          List.of(new ZDJStatusRecomputingSubmitted(tileDetectionTask.getJobId())));
    }
  }

  public boolean isFinished(List<TileDetectionTask> tasks) {
    return tasks.stream()
        .map(
            task ->
                FINISHED.equals(task.getStatus().getProgression())
                    && SUCCEEDED.equals(task.getStatus().getHealth()))
        .isParallel();
  }
}
