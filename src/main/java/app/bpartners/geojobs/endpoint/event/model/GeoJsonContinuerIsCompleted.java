package app.bpartners.geojobs.service.geojson;

import app.bpartners.geojobs.endpoint.event.model.PojaEvent;
import java.time.Duration;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class GeoJsonContinuerIsCompleted extends PojaEvent {
  private String id;
  private String fileKey;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(3L);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofMinutes(1L);
  }
}
