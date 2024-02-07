package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.gen.ZoneTilingJobStatusChanged;
import app.bpartners.geojobs.repository.model.JobStatus;
import app.bpartners.geojobs.repository.model.geo.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.geo.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.service.geo.tiling.TilingFinishedMailer;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class ZoneTilingJobStatusChangedService implements Consumer<ZoneTilingJobStatusChanged> {

  private TilingFinishedMailer tilingFinishedMailer;
  private EventProducer eventProducer;
  private ZoneDetectionJobService zoneDetectionJobService;

  @Override
  public void accept(ZoneTilingJobStatusChanged event) {
    ZoneTilingJob newJob = event.getNewJob();
    JobStatus status = newJob.getStatus();
    var progression = status.getProgression();
    var health = status.getHealth();

    var illegalFinishedMessage = "Cannot finish as unknown, event=" + event;
    var notFinishedMessage = "Not finished yet, nothing to do, event=" + event;
    var message =
        switch (progression) {
          case FINISHED -> switch (health) {
            case UNKNOWN -> throw new IllegalStateException(illegalFinishedMessage);
            case SUCCEEDED, FAILED -> processFinishedJob(newJob);
          };
          case PENDING, PROCESSING -> notFinishedMessage;
        };
    log.info(message);
  }

  private String processFinishedJob(ZoneTilingJob job) {
    // TODO: we surely would like to receive email only _once_ on finished,
    //  especially in case of failure.
    zoneDetectionJobService.saveZoneDetectionJobFromZTJ(job);
    tilingFinishedMailer.accept(job);
    return "Finished, mail sent, job=" + job;
  }
}
