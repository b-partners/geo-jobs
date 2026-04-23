package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.parcel.ParcelDetectionTaskCreated;
import app.bpartners.geojobs.job.repository.TaskRepository;
import app.bpartners.geojobs.job.service.TaskStatusService;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ParcelDetectionTaskCreatedService
    extends TaskCreatedService<ParcelDetectionTask, ParcelDetectionTaskCreated> {
  private final TaskStatusService<ParcelDetectionTask> taskStatusService;
  private final ParcelDetectionTaskConsumer parcelDetectionTaskConsumer;

  public ParcelDetectionTaskCreatedService(
      ParcelDetectionTaskConsumer taskConsumer,
      TaskStatusService<ParcelDetectionTask> taskStatusService,
      TaskRepository<ParcelDetectionTask> taskRepository) {
    super(taskConsumer, taskStatusService, taskRepository);
    this.taskStatusService = taskStatusService;
    this.parcelDetectionTaskConsumer = taskConsumer;
  }

  @Override
  public void accept(ParcelDetectionTaskCreated parcelDetectionTaskCreated) {
    long startTime = System.currentTimeMillis();
    var task = parcelDetectionTaskCreated.getTask();
    taskStatusService.process(task);
    parcelDetectionTaskConsumer.accept(task);
    long elapsedTime = startTime - System.currentTimeMillis();
    log.info(
        "{ \"operation\": \"ParcelDetectionTaskCreated\", \"parcelDetectionTaskId\": \"{}\","
            + " \"durationInMs\": \"{}\", \"isIntegrationTest\": \"{}\" }",
        parcelDetectionTaskCreated.getTask().getId(),
        elapsedTime,
        parcelDetectionTaskCreated.getTask().isIntegrationTest());
  }
}
