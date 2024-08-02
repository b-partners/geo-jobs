package app.bpartners.geojobs.endpoint.event.model;

import app.bpartners.geojobs.endpoint.rest.model.CreateFullDetection;
import app.bpartners.geojobs.repository.model.tiling.TilingTask;
import java.time.Duration;
import javax.annotation.processing.Generated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Generated("EventBridge")
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@EqualsAndHashCode
@ToString
public class TilingTaskFailed extends PojaEvent {
  private TilingTask task;
  private int attemptNb;
  private CreateFullDetection createFullDetection;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(1);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofMinutes(1);
  }
}
