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
  public TileDetectionTaskCreated(
      String zoneDetectionJobId,
      TileDetectionTask task,
      List<DetectableObjectConfiguration> detectableObjectConfigurations) {
    super(task);
    this.zoneDetectionJobId = zoneDetectionJobId;
    this.detectableObjectConfigurations = detectableObjectConfigurations;
  }

  private String zoneDetectionJobId;

  private List<DetectableObjectConfiguration> detectableObjectConfigurations;
}
