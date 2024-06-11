package app.bpartners.geojobs.service;

import app.bpartners.geojobs.endpoint.event.model.PojaEvent;
import app.bpartners.geojobs.job.model.Status;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StatusChangedHandler {
  public void handle(
      PojaEvent event,
      Status newStatus,
      Status oldStatus,
      StatusHandler onFinish,
      StatusHandler onFailed) {
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
        switch (oldStatus.getProgression()) {
          case PENDING, PROCESSING -> switch (newProgression) {
            case FINISHED -> switch (newHealth) {
              case UNKNOWN, RETRYING -> throw new IllegalStateException(illegalFinishedMessage);
              case SUCCEEDED -> onFinish.performAction();
              case FAILED -> onFailed.performAction();
            };
            case PENDING, PROCESSING -> notFinishedMessage;
          };
          case FINISHED -> doNothingMessage;
        };
    log.info(message);
  }
}
