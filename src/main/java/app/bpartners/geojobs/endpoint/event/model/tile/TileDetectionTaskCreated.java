package app.bpartners.geojobs.endpoint.event.model.tile;

import app.bpartners.geojobs.endpoint.event.model.TaskCreated;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import java.time.Duration;
import java.util.List;
import lombok.*;

@Getter
@EqualsAndHashCode(callSuper = true)
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

  private final String zoneDetectionJobId;

  private final List<DetectableObjectConfiguration> detectableObjectConfigurations;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(3);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofMinutes(1);
  }
}
