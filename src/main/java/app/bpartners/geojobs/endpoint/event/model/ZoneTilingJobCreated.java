package app.bpartners.geojobs.endpoint.event.model;

import app.bpartners.geojobs.endpoint.rest.model.CreateFullDetection;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
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
@EqualsAndHashCode(callSuper = false)
@ToString
public class ZoneTilingJobCreated extends PojaEvent {
  @JsonProperty("zoneTilingJob")
  private ZoneTilingJob zoneTilingJob;

  private CreateFullDetection createFullDetection;

  public ZoneTilingJobCreated(ZoneTilingJob zoneTilingJob) {
    this.zoneTilingJob = zoneTilingJob;
  }

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(3);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofMinutes(1);
  }
}
