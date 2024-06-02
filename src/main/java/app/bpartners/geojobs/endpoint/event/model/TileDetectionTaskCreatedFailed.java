package app.bpartners.geojobs.endpoint.event.model;

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
  public Duration maxDuration() {
    return Duration.ofMinutes(1);
  }

  @Override
  public Duration maxBackoffBetweenRetries() {
    return Duration.ofMinutes(1);
  }
}
