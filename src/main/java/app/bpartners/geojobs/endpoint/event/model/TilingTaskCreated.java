package app.bpartners.geojobs.endpoint.event.model;

import app.bpartners.geojobs.repository.model.tiling.TilingTask;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class TilingTaskCreated extends PojaEvent {
  @JsonProperty("tilingTask")
  private TilingTask task;

  @Override
  public Duration maxDuration() {
    return Duration.ofMinutes(10);
  }

  @Override
  public Duration maxBackoffBetweenRetries() {
    return Duration.ofMinutes(1);
  }
}
