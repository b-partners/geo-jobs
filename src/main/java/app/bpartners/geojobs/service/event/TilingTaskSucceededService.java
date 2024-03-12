package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.gen.TilingTaskSucceeded;
import app.bpartners.geojobs.job.service.RetryableTaskStatusService;
import app.bpartners.geojobs.repository.TilingTaskRepository;
import app.bpartners.geojobs.repository.model.tiling.TilingTask;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
@Slf4j
public class TilingTaskSucceededService implements Consumer<TilingTaskSucceeded> {

  private final TilingTaskRepository taskRepository;
  private final RetryableTaskStatusService<TilingTask, ZoneTilingJob> taskStatusService;

  @Override
  public void accept(TilingTaskSucceeded tilingTaskSucceeded) {
    var task = tilingTaskSucceeded.getTask();
    TilingTask savedTask = taskRepository.save(task);
    log.info("[DEBUG] Saved Tile {}", savedTask.getParcel().getParcelContent().getFirstTile());
    taskStatusService.succeed(task);
  }
}
