package app.bpartners.geojobs.endpoint.event.model;

import app.bpartners.geojobs.endpoint.event.EventStack;
import lombok.*;

@Data
@EqualsAndHashCode
@ToString
public class ZDJStatusRecomputingSubmitted extends JobStatusRecomputingSubmitted {
  private static final int MAX_CONSUMER_DURATION_VALUE = 300;
  private static final long DEFAULT_BACK_OFF_VALUE = 180L;

  public ZDJStatusRecomputingSubmitted(String jobId) {
    this.jobId = jobId;
    this.maxConsumerBackoffBetweenRetriesDurationValue = DEFAULT_BACK_OFF_VALUE;
    this.maxConsumerDurationValue = MAX_CONSUMER_DURATION_VALUE;
    this.attemptNb = 0;
  }

  public ZDJStatusRecomputingSubmitted(
      String jobId, Long maxConsumerBackoffBetweenRetriesDurationValue, Integer attemptNb) {
    super(
        jobId,
        MAX_CONSUMER_DURATION_VALUE,
        maxConsumerBackoffBetweenRetriesDurationValue,
        attemptNb);
  }

  @Override
  public EventStack getEventStack() {
    return EventStack.EVENT_STACK_2;
  }
}
