package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.ZDJStatusRecomputingSubmitted;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ZDJStatusRecomputingSubmittedService
    implements Consumer<ZDJStatusRecomputingSubmitted> {
  private final ZoneDetectionJobService jobService;

  @Override
  public void accept(ZDJStatusRecomputingSubmitted zdjStatusRecomputingSubmitted) {
    var jobId = zdjStatusRecomputingSubmitted.getJobId();
    var job = jobService.findById(jobId);
    jobService.recomputeStatus(job);
  }
}
