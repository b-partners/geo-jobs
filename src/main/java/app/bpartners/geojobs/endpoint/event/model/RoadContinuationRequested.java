package app.bpartners.geojobs.endpoint.event.model;

import java.io.File;
import java.time.Duration;
import lombok.*;

@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class RoadContinuationRequested extends PojaEvent {
  private final File geoJSON;
  private final Integer zoom;
  private final Integer imageSize;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(1L);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofSeconds(5L);
  }
}
