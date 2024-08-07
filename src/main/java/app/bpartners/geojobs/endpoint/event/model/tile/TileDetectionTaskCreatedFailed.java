package app.bpartners.geojobs.endpoint.event.model.tile;

import app.bpartners.geojobs.endpoint.event.model.PojaEvent;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import javax.annotation.processing.Generated;
import lombok.*;

@Generated("EventBridge")
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@EqualsAndHashCode
@ToString
public class TileDetectionTaskCreatedFailed extends PojaEvent {
  @JsonProperty("tileDetectionTask")
  private TileDetectionTaskCreated tileDetectionTaskCreated;

  @JsonProperty("attemptNb")
  private int attemptNb;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(1);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofMinutes(1);
  }
}
