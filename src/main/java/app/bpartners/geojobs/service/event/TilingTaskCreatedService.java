package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.service.event.TilingTaskConsumer.withNewStatus;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.tile.TilingTaskCreated;
import app.bpartners.geojobs.endpoint.event.model.tile.TilingTaskSucceeded;
import app.bpartners.geojobs.job.service.TaskStatusService;
import app.bpartners.geojobs.repository.model.tiling.TilingTask;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class TilingTaskCreatedService implements Consumer<TilingTaskCreated> {
  private final TaskStatusService<TilingTask> tilingTaskStatusService;
  private final TilingTaskConsumer tilingTaskConsumer;
  private final EventProducer eventProducer;

  @Override
  public void accept(TilingTaskCreated tilingTaskCreated) {
    TilingTask task = tilingTaskCreated.getTask();
    tilingTaskStatusService.process(task);

    tilingTaskConsumer.accept(task);

    eventProducer.accept(
        List.of(new TilingTaskSucceeded(withNewStatus(task, FINISHED, SUCCEEDED, null))));
  }
}
