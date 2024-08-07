package app.bpartners.geojobs.endpoint.event.model;

import app.bpartners.geojobs.endpoint.event.EventStack;
import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@Getter
@EqualsAndHashCode(callSuper = false)
@ToString
public class AutoTaskStatisticRecomputingSubmitted extends StatisticRecomputingSubmitted {
  private static final long MAX_CONSUMER_DURATION_VALUE = 300L;
  private static final long DEFAULT_BACK_OFF_VALUE = 180L;

  public AutoTaskStatisticRecomputingSubmitted(String jobId) {
    super(jobId, MAX_CONSUMER_DURATION_VALUE, DEFAULT_BACK_OFF_VALUE);
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
