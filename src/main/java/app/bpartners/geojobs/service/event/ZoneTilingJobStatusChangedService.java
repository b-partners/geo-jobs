package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.ZoneTilingJobStatusChanged;
import app.bpartners.geojobs.repository.FullDetectionRepository;
import app.bpartners.geojobs.repository.model.detection.FullDetection;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.JobFinishedMailer;
import app.bpartners.geojobs.service.StatusChangedHandler;
import app.bpartners.geojobs.service.StatusHandler;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class ZoneTilingJobStatusChangedService implements Consumer<ZoneTilingJobStatusChanged> {
  private final JobFinishedMailer<ZoneTilingJob> tilingFinishedMailer;
  private final ZoneDetectionJobService zoneDetectionJobService;
  private final StatusChangedHandler statusChangedHandler;
  private final FullDetectionRepository fullDetectionRepository;

  @Override
  public void accept(ZoneTilingJobStatusChanged event) {
    var oldJob = event.getOldJob();
    var newJob = event.getNewJob();

    statusChangedHandler.handle(
        event,
        newJob.getStatus(),
        oldJob.getStatus(),
        new OnFinishedHandler(
            tilingFinishedMailer, zoneDetectionJobService, newJob, fullDetectionRepository),
        new OnFinishedHandler(
            tilingFinishedMailer, zoneDetectionJobService, newJob, fullDetectionRepository));
  }

  private record OnFinishedHandler(
      JobFinishedMailer<ZoneTilingJob> tilingFinishedMailer,
      ZoneDetectionJobService zoneDetectionJobService,
      ZoneTilingJob ztj,
      FullDetectionRepository fullDetectionRepository)
      implements StatusHandler {

    @Override
    public String performAction() {
      var zdj = zoneDetectionJobService.saveZDJFromZTJ(ztj);
      FullDetection fullDetection = fullDetectionRepository.findByZtjId(ztj.getId());
      if (fullDetection != null) {
        fullDetection.setZdjId(zdj.getId());
        fullDetectionRepository.save(fullDetection);
      }
      tilingFinishedMailer.accept(ztj);
      return "Finished, mail sent, ztj=" + ztj;
    }
  }
}
