package app.bpartners.geojobs.service.event.annotation.delivery;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationDeliveryJobStatusChanged;
import app.bpartners.geojobs.repository.model.annotation.AnnotationDeliveryJob;
import app.bpartners.geojobs.service.StatusChangedHandler;
import app.bpartners.geojobs.service.StatusHandler;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class AnnotationDeliveryJobStatusChangedService
    implements Consumer<AnnotationDeliveryJobStatusChanged> {
  private final EventProducer eventProducer;
  private final StatusChangedHandler statusChangedHandler;

  @Override
  public void accept(AnnotationDeliveryJobStatusChanged event) {
    var oldJob = event.getOldJob();
    var newJob = event.getNewJob();

    OnFinishedHandler onFinishedHandler = new OnFinishedHandler(eventProducer, newJob);

    statusChangedHandler.handle(
        event, newJob.getStatus(), oldJob.getStatus(), onFinishedHandler, onFinishedHandler);
  }

  private record OnFinishedHandler(EventProducer eventProducer, AnnotationDeliveryJob newJob)
      implements StatusHandler {

    @Override
    public String performAction() {
      String detectionJobId = newJob.getDetectionJobId();

      // TODO eventProducer.accept(List.of());

      return "Annotation Delivery Job (id"
          + newJob.getId()
          + ") finished with status "
          + newJob.getStatus();
    }
  }
}
