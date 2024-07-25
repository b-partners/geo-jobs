package app.bpartners.geojobs.endpoint.event.model;

import app.bpartners.geojobs.endpoint.event.EventStack;
import java.time.Duration;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@EqualsAndHashCode(callSuper = false)
@ToString
public class AutoTaskStatisticRecomputingSubmitted extends PojaEvent {
  private static final long MAX_CONSUMER_DURATION_VALUE = 300L;
  private static final long DEFAULT_BACK_OFF_VALUE = 180L;
  private String jobId;
  private long maxConsumerDurationValue;
  private long maxConsumerBackoffBetweenRetriesDurationValue;
  private int attemptNb;

  public AutoTaskStatisticRecomputingSubmitted(String jobId) {
    this.jobId = jobId;
    this.maxConsumerDurationValue = MAX_CONSUMER_DURATION_VALUE;
    this.maxConsumerBackoffBetweenRetriesDurationValue = DEFAULT_BACK_OFF_VALUE;
    this.attemptNb = 0;
  }

  public AutoTaskStatisticRecomputingSubmitted(
      String jobId, long maxConsumerBackoffBetweenRetriesDurationValue, int attemptNb) {
    this.jobId = jobId;
    this.maxConsumerDurationValue = MAX_CONSUMER_DURATION_VALUE;
    this.maxConsumerBackoffBetweenRetriesDurationValue =
        maxConsumerBackoffBetweenRetriesDurationValue;
    this.attemptNb = attemptNb;
  }

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofSeconds(maxConsumerDurationValue);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofSeconds(maxConsumerBackoffBetweenRetriesDurationValue);
  }

  @Override
  public EventStack getEventStack() {
    return EventStack.EVENT_STACK_2;
  }
}
