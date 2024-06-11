package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.ZoneTilingJobStatusChanged;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.StatusChangedHandler;
import app.bpartners.geojobs.service.StatusHandler;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.service.tiling.TilingFinishedMailer;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class ZoneTilingJobStatusChangedService implements Consumer<ZoneTilingJobStatusChanged> {
  private final TilingFinishedMailer tilingFinishedMailer;
  private final ZoneDetectionJobService zoneDetectionJobService;
  private final StatusChangedHandler statusChangedHandler;

  @Override
  public void accept(ZoneTilingJobStatusChanged event) {
    var oldJob = event.getOldJob();
    var newJob = event.getNewJob();

    statusChangedHandler.handle(
        event,
        newJob.getStatus(),
        oldJob.getStatus(),
        new OnFinishedHandler(tilingFinishedMailer, zoneDetectionJobService, newJob),
        new OnFinishedHandler(tilingFinishedMailer, zoneDetectionJobService, newJob));
  }

  private record OnFinishedHandler(
      TilingFinishedMailer tilingFinishedMailer,
      ZoneDetectionJobService zoneDetectionJobService,
      ZoneTilingJob ztj)
      implements StatusHandler {

    @Override
    public String performAction() {
      zoneDetectionJobService.saveZDJFromZTJ(ztj);
      tilingFinishedMailer.accept(ztj);
      return "Finished, mail sent, ztj=" + ztj;
    }
  }
}
