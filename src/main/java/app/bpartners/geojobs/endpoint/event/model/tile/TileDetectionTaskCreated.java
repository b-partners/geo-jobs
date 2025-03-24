package app.bpartners.geojobs.endpoint.event.model.tile;

import app.bpartners.geojobs.endpoint.event.model.PojaEvent;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import java.util.List;
import javax.annotation.processing.Generated;
import lombok.*;

@Generated("EventBridge")
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@EqualsAndHashCode
@ToString
public class TileDetectionTaskCreated extends PojaEvent {
  private String zoneDetectionJobId;

  @JsonProperty("tileDetectionTask")
  private TileDetectionTask tileDetectionTask;

  @JsonProperty("detectableObjectConfigurations")
  private List<DetectableObjectConfiguration> detectableObjectConfigurations;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(3);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofMinutes(1);
  }
}
