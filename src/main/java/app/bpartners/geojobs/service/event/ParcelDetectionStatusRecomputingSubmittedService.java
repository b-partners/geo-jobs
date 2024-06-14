package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.ParcelDetectionStatusRecomputingSubmitted;
import app.bpartners.geojobs.service.detection.ParcelDetectionJobService;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ParcelDetectionStatusRecomputingSubmittedService
    implements Consumer<ParcelDetectionStatusRecomputingSubmitted> {
  private final ParcelDetectionJobService parcelDetectionJobService;

  @Override
  public void accept(ParcelDetectionStatusRecomputingSubmitted event) {
    var parcelDetectionJobId = event.getParcelDetectionJobId();
    var parcelDetectionJob = parcelDetectionJobService.findById(parcelDetectionJobId);
    parcelDetectionJobService.recomputeStatus(parcelDetectionJob);
  }
}
