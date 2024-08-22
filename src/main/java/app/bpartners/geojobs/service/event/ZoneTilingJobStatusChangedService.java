package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.zone.ZoneDetectionJobCreated;
import app.bpartners.geojobs.endpoint.event.model.zone.ZoneTilingJobStatusChanged;
import app.bpartners.geojobs.repository.FullDetectionRepository;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.JobFinishedMailer;
import app.bpartners.geojobs.service.StatusChangedHandler;
import app.bpartners.geojobs.service.StatusHandler;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import java.util.List;
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
  private final EventProducer eventProducer;

  @Override
  public void accept(ZoneTilingJobStatusChanged event) {
    var oldJob = event.getOldJob();
    var newJob = event.getNewJob();

    var onFinishHandler =
        new OnFinishedHandler(
            eventProducer,
            tilingFinishedMailer,
            zoneDetectionJobService,
            newJob,
            fullDetectionRepository);
    statusChangedHandler.handle(
        event, newJob.getStatus(), oldJob.getStatus(), onFinishHandler, onFinishHandler);
  }

  private record OnFinishedHandler(
      EventProducer eventProducer,
      JobFinishedMailer<ZoneTilingJob> tilingFinishedMailer,
      ZoneDetectionJobService zoneDetectionJobService,
      ZoneTilingJob ztj,
      FullDetectionRepository fullDetectionRepository)
      implements StatusHandler {

    @Override
    public String performAction() {
      var zdj = zoneDetectionJobService.saveZDJFromZTJ(ztj);
      var optionalFullDetection = fullDetectionRepository.findByZtjId(ztj.getId());
      // For now, only fullDetection process triggers ZDJ processing
      if (optionalFullDetection.isPresent()) {
        var fullDetection = optionalFullDetection.get();
        fullDetection.setZdjId(zdj.getId());
        fullDetectionRepository.save(fullDetection);
        eventProducer.accept(
            List.of(ZoneDetectionJobCreated.builder().zoneDetectionJob(zdj).build()));
      }
      tilingFinishedMailer.accept(ztj);
      return "Finished, mail sent, ztj=" + ztj;
    }
  }
}
