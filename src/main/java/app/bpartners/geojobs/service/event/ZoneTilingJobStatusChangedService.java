package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.gen.ZoneTilingJobStatusChanged;
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
  private ZoneDetectionJobService zoneDetectionJobService;

  @Override
  public void accept(ZoneTilingJobStatusChanged event) {
    var newJob = event.getNewJob();
    var newStatus = newJob.getStatus();
    var newProgression = newStatus.getProgression();
    var newHealth = newStatus.getHealth();

    var oldJob = event.getOldJob();
    var oldStatus = oldJob.getStatus();
    var oldProgression = oldStatus.getProgression();

    var illegalFinishedMessage = "Cannot finish as unknown, event=" + event;
    var notFinishedMessage = "Not finished yet, nothing to do, event=" + event;
    var doNothingMessage = "Old job already finished, do nothing";
    var message =
        switch (oldProgression) {
          case PENDING, PROCESSING -> switch (newProgression) {
            case FINISHED -> switch (newHealth) {
              case UNKNOWN -> throw new IllegalStateException(illegalFinishedMessage);
              case SUCCEEDED, FAILED -> handleFinishedJob(newJob);
            };
            case PENDING, PROCESSING -> notFinishedMessage;
          };
          case FINISHED -> doNothingMessage;
        };
    log.info(message);
  }

  private String handleFinishedJob(ZoneTilingJob ztj) {
    zoneDetectionJobService.saveZDJFromZTJ(ztj);
    tilingFinishedMailer.accept(ztj);
    return "Finished, mail sent, ztj=" + ztj;
  }
}
