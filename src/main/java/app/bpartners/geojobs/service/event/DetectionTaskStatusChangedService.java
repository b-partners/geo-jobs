package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.DetectionTaskStatusChanged;
import app.bpartners.geojobs.endpoint.event.model.ParcelDetectionTaskSucceeded;
import app.bpartners.geojobs.job.service.TaskStatusService;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionTask;
import app.bpartners.geojobs.service.StatusChangedHandler;
import app.bpartners.geojobs.service.StatusHandler;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class DetectionTaskStatusChangedService implements Consumer<DetectionTaskStatusChanged> {
  private final EventProducer eventProducer;
  private final TaskStatusService<ParcelDetectionTask> taskStatusService;
  private final StatusChangedHandler statusChangedHandler;

  @Override
  public void accept(DetectionTaskStatusChanged event) {
    var oldTask = event.getOldTask();
    var newTask = event.getNewTask();

    statusChangedHandler.handle(
        event,
        newTask.getStatus(),
        oldTask.getStatus(),
        new OnSucceededHandler(eventProducer, newTask),
        new OnFailedHandler(taskStatusService, newTask));
  }

  private record OnSucceededHandler(
      EventProducer eventProducer, ParcelDetectionTask parcelDetectionTask)
      implements StatusHandler {

    @Override
    public String performAction() {
      eventProducer.accept(List.of(new ParcelDetectionTaskSucceeded(parcelDetectionTask)));
      return "Finished task="
          + parcelDetectionTask
          + ", now computing new status of job(id="
          + parcelDetectionTask.getJobId()
          + ")";
    }
  }

  private record OnFailedHandler(
      TaskStatusService<ParcelDetectionTask> taskStatusService,
      ParcelDetectionTask parcelDetectionTask)
      implements StatusHandler {

    @Override
    public String performAction() {
      // TODO: if necessary, retry detectionTask directly with nbAttempt
      // eventProducer.accept(List.of(new DetectionTaskFailed(newTask, nbAttempt)));
      taskStatusService.fail(parcelDetectionTask);
      String message = "Failed to process task=" + parcelDetectionTask;
      log.error(message);
      return message;
    }
  }
}
