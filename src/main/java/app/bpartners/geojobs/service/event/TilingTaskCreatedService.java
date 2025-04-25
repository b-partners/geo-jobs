package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.tile.TilingTaskCreated;
import app.bpartners.geojobs.job.repository.TaskRepository;
import app.bpartners.geojobs.job.service.TaskStatusService;
import app.bpartners.geojobs.repository.model.tiling.ParcelTilingTask;
import app.bpartners.geojobs.service.TaskConsumer;
import org.springframework.stereotype.Service;

@Service
public class TilingTaskCreatedService
    extends TaskCreatedService<ParcelTilingTask, TilingTaskCreated> {

  public TilingTaskCreatedService(
      TaskConsumer<ParcelTilingTask> taskConsumer,
      TaskStatusService<ParcelTilingTask> taskStatusService,
      TaskRepository<ParcelTilingTask> taskRepository) {
    super(taskConsumer, taskStatusService, taskRepository);
  }

  @Override
  public void accept(TilingTaskCreated tilingTaskCreated) {
    super.accept(tilingTaskCreated);
  }
}
