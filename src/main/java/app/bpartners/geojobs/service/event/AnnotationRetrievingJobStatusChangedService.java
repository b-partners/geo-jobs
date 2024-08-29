package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationRetrievingJobStatusChanged;
import app.bpartners.geojobs.endpoint.event.model.status.HumanZDJStatusRecomputingSubmitted;
import app.bpartners.geojobs.job.repository.JobStatusRepository;
import app.bpartners.geojobs.repository.model.AnnotationRetrievingJob;
import app.bpartners.geojobs.service.AnnotationRetrievingJobService;
import app.bpartners.geojobs.service.StatusChangedHandler;
import app.bpartners.geojobs.service.StatusHandler;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.service.geojson.GeoJsonConversionInitiationService;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class AnnotationRetrievingJobStatusChangedService
    implements Consumer<AnnotationRetrievingJobStatusChanged> {
  private final EventProducer eventProducer;
  private final StatusChangedHandler statusChangedHandler;
  private final ZoneDetectionJobService zoneDetectionJobService;
  private final AnnotationRetrievingJobService retrievingJobService;
  private final JobStatusRepository jobStatusRepository;
  private final GeoJsonConversionInitiationService geoJsonConversionInitiationService;

  @Override
  public void accept(AnnotationRetrievingJobStatusChanged event) {
    var oldJob = event.getOldJob();
    var newJob = event.getNewJob();

    OnFinishedHandler onFinishedHandler = new OnFinishedHandler(eventProducer, newJob);

    statusChangedHandler.handle(
        event, newJob.getStatus(), oldJob.getStatus(), onFinishedHandler, onFinishedHandler);
  }

  private record OnFinishedHandler(EventProducer eventProducer, AnnotationRetrievingJob newJob)
      implements StatusHandler {

    @Override
    public String performAction() {
      String detectionJobId = newJob.getDetectionJobId();

      eventProducer.accept(List.of(new HumanZDJStatusRecomputingSubmitted(detectionJobId)));

      return "AnnotationRetrievedJob (id"
          + newJob.getId()
          + ") finished with status "
          + newJob.getStatus();
    }
  }
}
