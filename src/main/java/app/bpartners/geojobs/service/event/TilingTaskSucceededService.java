package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.TilingTaskSucceeded;
import app.bpartners.geojobs.endpoint.event.model.ZTJStatusRecomputingSubmitted;
import app.bpartners.geojobs.job.service.TaskStatusService;
import app.bpartners.geojobs.repository.TilingTaskRepository;
import app.bpartners.geojobs.repository.model.tiling.TilingTask;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
@Slf4j
public class TilingTaskSucceededService implements Consumer<TilingTaskSucceeded> {

  private final TilingTaskRepository taskRepository;
  private final TaskStatusService<TilingTask> taskStatusService;
  private final EventProducer eventProducer;

  public void accept(TilingTaskSucceeded tilingTaskSucceeded) {
    var task = tilingTaskSucceeded.getTask();
    taskRepository.save(task);
    taskStatusService.succeed(task);
    var jobTasks = taskRepository.findAllByJobId(task.getJobId());
    if (isFinished(jobTasks)) {
      eventProducer.accept(
          List.of(
              new ZTJStatusRecomputingSubmitted(
                  task.getJobId(), tilingTaskSucceeded.getFullDetection())));
    }
  }

  private boolean isFinished(List<TilingTask> tilingTasks) {
    return tilingTasks.stream()
        .allMatch(
            tilingTask ->
                FINISHED.equals(tilingTask.getStatus().getProgression())
                    && SUCCEEDED.equals(tilingTask.getStatus().getHealth()));
  }
}
