package app.bpartners.geojobs.endpoint.event.model;

import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
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
  @JsonProperty("tileDetectionTask")
  private TileDetectionTask tileDetectionTask;

  @JsonProperty("detectableTypes")
  private List<DetectableType> detectableTypes;

  @Override
  public Duration maxDuration() {
    return Duration.ofMinutes(3);
  }

  @Override
  public Duration maxBackoffBetweenRetries() {
    return Duration.ofMinutes(1);
  }
}
