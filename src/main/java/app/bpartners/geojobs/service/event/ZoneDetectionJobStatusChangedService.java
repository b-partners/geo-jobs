package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.ZoneDetectionJobStatusChanged;
import app.bpartners.geojobs.endpoint.event.model.ZoneDetectionJobSucceeded;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.service.StatusChangedHandler;
import app.bpartners.geojobs.service.StatusHandler;
import app.bpartners.geojobs.service.detection.DetectionFinishedMailer;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class ZoneDetectionJobStatusChangedService
    implements Consumer<ZoneDetectionJobStatusChanged> {
  private final DetectionFinishedMailer mailer;
  private final EventProducer eventProducer;
  private final StatusChangedHandler statusChangedHandler;

  @Override
  public void accept(ZoneDetectionJobStatusChanged event) {
    var oldJob = event.getOldJob();
    var newJob = event.getNewJob();

    statusChangedHandler.handle(
        event,
        newJob.getStatus(),
        oldJob.getStatus(),
        new OnSucceededHandler(mailer, eventProducer, newJob),
        new OnFailedHandler(newJob));
  }

  private record OnSucceededHandler(
      DetectionFinishedMailer mailer, EventProducer eventProducer, ZoneDetectionJob zdj)
      implements StatusHandler {

    @Override
    public String performAction() {
      mailer.accept(zdj);
      eventProducer.accept(
          List.of(ZoneDetectionJobSucceeded.builder().succeededJobId(zdj.getId()).build()));
      return "Finished, mail sent, ztj=" + zdj;
    }
  }

  private record OnFailedHandler(ZoneDetectionJob zdj) implements StatusHandler {

    @Override
    public String performAction() {
      throw new ApiException(SERVER_EXCEPTION, "Failed to process zdj=" + zdj.getId());
    }
  }
}
