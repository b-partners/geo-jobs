package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.ZTJStatusRecomputingSubmitted;
import app.bpartners.geojobs.service.tiling.ZoneTilingJobService;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class ZTJStatusRecomputingSubmittedService
    implements Consumer<ZTJStatusRecomputingSubmitted> {
  private final ZoneTilingJobService zoneTilingJobService;

  @Override
  public void accept(ZTJStatusRecomputingSubmitted ztjStatusRecomputingSubmitted) {
    var job = zoneTilingJobService.findById(ztjStatusRecomputingSubmitted.getJobId());
    zoneTilingJobService.recomputeStatus(job);
  }
}
