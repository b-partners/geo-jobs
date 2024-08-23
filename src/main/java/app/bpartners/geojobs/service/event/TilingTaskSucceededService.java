package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.tile.TilingTaskSucceeded;
import app.bpartners.geojobs.job.service.TaskStatusService;
import app.bpartners.geojobs.repository.TilingTaskRepository;
import app.bpartners.geojobs.repository.model.tiling.TilingTask;
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

  @Override
  public void accept(TilingTaskSucceeded tilingTaskSucceeded) {
    var task = tilingTaskSucceeded.getTask();
    taskRepository.save(task);
    taskStatusService.succeed(task);
  }
}
