package app.bpartners.geojobs.endpoint.event.model;

import static java.time.temporal.ChronoUnit.MINUTES;

import app.bpartners.geojobs.endpoint.event.EventStack;
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
public class ZTJStatusRecomputingSubmitted extends PojaEvent {
  private String jobId;
  private long maxConsumerBackoffBetweenRetriesDurationValue;
  private int attemptNb;

  public ZTJStatusRecomputingSubmitted(String jobId) {
    this.jobId = jobId;
    this.maxConsumerBackoffBetweenRetriesDurationValue = 5L;
    this.attemptNb = 0;
  }

  @Override
  public Duration maxConsumerDuration() {
    return Duration.of(10, MINUTES);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.of(maxConsumerBackoffBetweenRetriesDurationValue, MINUTES);
  }

  @Override
  public EventStack getEventStack() {
    return EventStack.EVENT_STACK_2;
  }
}
