package app.bpartners.geojobs.endpoint.event.model;

import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

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

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofSeconds(maxConsumerDurationValue);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    // u_n = u_0 * q^n
    // u_0: maxConsumerBackoffBetweenRetriesDurationValue
    // q: 2
    // n: attemptNb()
    return Duration.ofSeconds(
        maxConsumerBackoffBetweenRetriesDurationValue * (int) Math.pow(2, getAttemptNb()));
  }
}
