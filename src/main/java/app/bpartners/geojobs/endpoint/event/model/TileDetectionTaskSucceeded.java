package app.bpartners.geojobs.endpoint.event.model;

import app.bpartners.geojobs.repository.model.TileDetectionTask;
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
public class TileDetectionTaskSucceeded extends PojaEvent {
  @JsonProperty("tileDetectionTask")
  private TileDetectionTask tileDetectionTask;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(1);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofMinutes(1);
  }
}
