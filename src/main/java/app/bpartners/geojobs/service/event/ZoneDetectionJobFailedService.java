package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.zone.ZoneDetectionJobFailed;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class ZoneDetectionJobFailedService implements Consumer<ZoneDetectionJobFailed> {
  private final ZoneDetectionFinishedConsumer finishedConsumer;
  private final ZoneDetectionJobService jobService;

  @Override
  public void accept(ZoneDetectionJobFailed event) {
    var failedJobId = event.getFailedJobId();
    var detectionJob = jobService.findById(failedJobId);
    log.warn(
        "ZDJ(id={}) failed with status {}, processing to annotator succeeded task anyway",
        failedJobId,
        detectionJob.getStatus());

    finishedConsumer.accept(failedJobId);
  }
}
