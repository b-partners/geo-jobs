package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.repository.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.repository.model.Status.ProgressionStatus.PROCESSING;

import app.bpartners.geojobs.endpoint.event.gen.ZoneDetectionJobStatusChanged;
import app.bpartners.geojobs.service.geo.detection.DetectionFinishedMailer;
import app.bpartners.geojobs.service.geo.detection.ZoneDetectionJobService;
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
  private final ZoneDetectionJobService zoneDetectionJobService;

  @Override
  public void accept(ZoneDetectionJobStatusChanged zoneDetectionJobStatusChanged) {
    var oldJob = zoneDetectionJobStatusChanged.getOldJob();
    var newJob = zoneDetectionJobStatusChanged.getNewJob();
    var oldProgression = oldJob.getStatus().getProgression();
    var newProgression = newJob.getStatus().getProgression();

    if (oldProgression == PROCESSING && newProgression == FINISHED) {
      mailer.accept(newJob);
      zoneDetectionJobService.handleInDoubtObjects(newJob);
    }
  }
}
