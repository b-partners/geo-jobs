package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.ZDJStatusRecomputingSubmitted;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionTask;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ZDJStatusRecomputingSubmittedService
    implements Consumer<ZDJStatusRecomputingSubmitted> {
  private final JobStatusRecomputingSubmittedService<
          ZoneDetectionJob, ParcelDetectionTask, ZDJStatusRecomputingSubmitted>
      service;

  public ZDJStatusRecomputingSubmittedService(
      ZoneDetectionJobService jobService, EventProducer eventProducer) {
    this.service = new JobStatusRecomputingSubmittedService<>(eventProducer, jobService);
  }

  @Override
  public void accept(ZDJStatusRecomputingSubmitted event) {
    service.accept(event);
  }
}
