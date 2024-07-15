package app.bpartners.geojobs.endpoint.event.model;

import java.time.Duration;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@EqualsAndHashCode
@ToString
public class JobStatusRecomputingSubmitted extends PojaEvent {
  protected String jobId;
  protected long maxConsumerDurationValue;
  protected long maxConsumerBackoffBetweenRetriesDurationValue;
  protected int attemptNb;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofSeconds(maxConsumerDurationValue);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofSeconds(maxConsumerBackoffBetweenRetriesDurationValue);
  }
}
