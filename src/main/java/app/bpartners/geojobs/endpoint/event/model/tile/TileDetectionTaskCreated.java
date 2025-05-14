package app.bpartners.geojobs.endpoint.event.model.tile;

import app.bpartners.geojobs.endpoint.event.model.TaskCreated;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import java.util.List;
import lombok.*;

@Getter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ToString
public class TileDetectionTaskCreated extends TaskCreated<TileDetectionTask> {
  private String zoneDetectionJobId;
  private String address;
  private List<DetectableObjectConfiguration> detectableObjectConfigurations;

  public TileDetectionTaskCreated(
      String zoneDetectionJobId,
      TileDetectionTask task,
      List<DetectableObjectConfiguration> detectableObjectConfigurations,
      String address) {
    super(task);
    this.zoneDetectionJobId = zoneDetectionJobId;
    this.detectableObjectConfigurations = detectableObjectConfigurations;
    this.address = address;
  }
}
