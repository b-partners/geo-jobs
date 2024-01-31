package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.gen.DetectionJobCreated;
import app.bpartners.geojobs.service.geo.detection.ZoneDetectionJobService;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class DetectionJobCreatedService implements Consumer<DetectionJobCreated> {
  private final ZoneDetectionJobService zoneDetectionJobService;

  @Override
  public void accept(DetectionJobCreated detectionJobCreated) {
    zoneDetectionJobService.saveDetectionJobFromTilingTask(detectionJobCreated.getTilingTask());
  }
}
