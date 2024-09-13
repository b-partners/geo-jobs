package app.bpartners.geojobs.service.event.annotation.delivery;

import static app.bpartners.gen.annotator.endpoint.rest.model.JobStatus.STARTED;

import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationDeliveryJobStatusChanged;
import app.bpartners.geojobs.repository.model.annotation.AnnotationDeliveryJob;
import app.bpartners.geojobs.service.StatusChangedHandler;
import app.bpartners.geojobs.service.StatusHandler;
import app.bpartners.geojobs.service.annotator.AnnotationService;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class AnnotationDeliveryJobStatusChangedService
    implements Consumer<AnnotationDeliveryJobStatusChanged> {
  private final StatusChangedHandler statusChangedHandler;
  private final AnnotationService annotationService;

  @Override
  public void accept(AnnotationDeliveryJobStatusChanged event) {
    var oldJob = event.getOldJob();
    var newJob = event.getNewJob();

    OnFinishedHandler onFinishedHandler = new OnFinishedHandler(annotationService, newJob);

    statusChangedHandler.handle(
        event, newJob.getStatus(), oldJob.getStatus(), onFinishedHandler, onFinishedHandler);
  }

  private record OnFinishedHandler(
      AnnotationService annotationService, AnnotationDeliveryJob newJob) implements StatusHandler {

    @Override
    public String performAction() {
      var detectionJobId = newJob.getDetectionJobId();
      var annotationJobId = newJob.getAnnotationJobId();
      var annotationJobName = newJob.getAnnotationJobName();
      var labels = newJob.getLabels();

      annotationService.saveAnnotationJob(
          detectionJobId, annotationJobId, annotationJobName, labels, STARTED);

      return "Annotation Delivery Job (id"
          + newJob.getId()
          + ") finished with status "
          + newJob.getStatus();
    }
  }
}
