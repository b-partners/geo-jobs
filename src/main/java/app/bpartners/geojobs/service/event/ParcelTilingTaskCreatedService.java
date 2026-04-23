package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.tile.ParcelTilingTaskCreated;
import app.bpartners.geojobs.job.repository.TaskRepository;
import app.bpartners.geojobs.job.service.TaskStatusService;
import app.bpartners.geojobs.repository.model.tiling.ParcelTilingTask;
import app.bpartners.geojobs.service.TaskConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ParcelTilingTaskCreatedService
    extends TaskCreatedService<ParcelTilingTask, ParcelTilingTaskCreated> {

  public ParcelTilingTaskCreatedService(
      TaskConsumer<ParcelTilingTask> taskConsumer,
      TaskStatusService<ParcelTilingTask> taskStatusService,
      TaskRepository<ParcelTilingTask> taskRepository) {
    super(taskConsumer, taskStatusService, taskRepository);
  }

  @Override
  public void accept(ParcelTilingTaskCreated parcelTilingTaskCreated) {
    long startTime = System.currentTimeMillis();
    super.accept(parcelTilingTaskCreated);
    long elapsedTime = System.currentTimeMillis() - startTime;
    log.info(
        "{ \"operation\": \"ParcelTilingTaskCreated\",  \"parcelId\": \"{}\",  \"durationInMs\":"
            + " \"{}\", \"isIntegrationTest\": \"{}\" }",
        parcelTilingTaskCreated.getTask().getParcelId(),
        elapsedTime,
        parcelTilingTaskCreated.isIntegrationTest());
  }
}
