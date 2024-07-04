package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.ZTJStatusRecomputingSubmitted;
import app.bpartners.geojobs.repository.model.tiling.TilingTask;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.tiling.ZoneTilingJobService;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;

@Service
public class ZTJStatusRecomputingSubmittedService
    implements Consumer<ZTJStatusRecomputingSubmitted> {
  private final JobStatusRecomputingSubmittedService<
          ZoneTilingJob, TilingTask, ZTJStatusRecomputingSubmitted>
      service;

  public ZTJStatusRecomputingSubmittedService(
      ZoneTilingJobService jobService, EventProducer eventProducer) {
    this.service = new JobStatusRecomputingSubmittedService<>(eventProducer, jobService);
  }

  @Override
  public void accept(ZTJStatusRecomputingSubmitted event) {
    service.accept(event);
  }
}
