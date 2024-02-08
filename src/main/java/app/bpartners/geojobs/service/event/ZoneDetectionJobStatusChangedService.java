package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.repository.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.repository.model.Status.ProgressionStatus.PROCESSING;

import app.bpartners.geojobs.endpoint.event.gen.ZoneDetectionJobStatusChanged;
import app.bpartners.geojobs.service.geo.detection.DetectionFinishedMailer;
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

  @Override
  public void accept(ZoneDetectionJobStatusChanged zoneDetectionJobStatusChanged) {
    var oldJob = zoneDetectionJobStatusChanged.getOldJob();
    var newJob = zoneDetectionJobStatusChanged.getNewJob();
    if (oldJob.getStatus().getProgression() == PROCESSING
        && newJob.getStatus().getProgression() == FINISHED) {
      mailer.accept(newJob);
    }
  }
}
