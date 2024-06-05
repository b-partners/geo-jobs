package app.bpartners.geojobs.endpoint.event.model;

import static java.time.temporal.ChronoUnit.MINUTES;

import java.time.Duration;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@EqualsAndHashCode
@ToString
public class ZDJStatusRecomputingSubmitted extends PojaEvent {
  private String jobId;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.of(10, MINUTES);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.of(1, MINUTES);
  }
}
