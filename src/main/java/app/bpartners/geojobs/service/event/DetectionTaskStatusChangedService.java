package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.gen.DetectionTaskStatusChanged;
import app.bpartners.geojobs.endpoint.event.gen.DetectionTaskSucceeded;
import app.bpartners.geojobs.job.service.RetryableTaskStatusService;
import app.bpartners.geojobs.repository.model.detection.DetectionTask;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
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
  private final RetryableTaskStatusService<DetectionTask, ZoneDetectionJob> taskStatusService;

  @Override
  public void accept(DetectionTaskStatusChanged event) {
    var oldTask = event.getOldTask();
    var oldStatus = oldTask.getStatus();
    var oldProgression = oldStatus.getProgression();

    var newTask = event.getNewTask();
    var newStatus = newTask.getStatus();
    var newProgression = newStatus.getProgression();
    var newHealth = newStatus.getHealth();

    if (oldStatus.equals(newStatus)) {
      log.info("Status did not change, yet change event received: event=" + event);
      return;
    }

    var illegalFinishedMessage = "Cannot finish as unknown or retrying, event=" + event;
    var notFinishedMessage = "Not finished yet, nothing to do, event=" + event;
    var doNothingMessage = "Old task already finished, do nothing";
    var message =
        switch (oldProgression) {
          case PENDING, PROCESSING -> switch (newProgression) {
            case FINISHED -> switch (newHealth) {
              case UNKNOWN, RETRYING -> throw new IllegalStateException(illegalFinishedMessage);
              case SUCCEEDED -> handleFinishedTask(newTask);
              case FAILED -> handleFinishedFailedTask(newTask);
            };
            case PENDING, PROCESSING -> notFinishedMessage;
          };
          case FINISHED -> doNothingMessage;
        };
    log.info(message);
  }

  private String handleFinishedTask(DetectionTask newTask) {
    eventProducer.accept(List.of(new DetectionTaskSucceeded(newTask)));
    return "Finished task="
        + newTask
        + ", now computing new status of job(id="
        + newTask.getJobId()
        + ")";
  }

  private String handleFinishedFailedTask(DetectionTask newTask) {
    // TODO: if necessary, retry detectionTask directly with nbAttempt
    // eventProducer.accept(List.of(new DetectionTaskFailed(newTask, nbAttempt)));
    taskStatusService.fail(newTask);
    String message = "Failed to process task=" + newTask;
    log.error(message);
    return message;
  }
}
