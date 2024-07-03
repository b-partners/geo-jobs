package app.bpartners.geojobs.endpoint.event.model;


import app.bpartners.geojobs.endpoint.event.EventStack;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode
@ToString
public class ZTJStatusRecomputingSubmitted extends JobStatusRecomputingSubmitted {
  private static final long MAX_CONSUMER_DURATION_VALUE = 10L;
  private static final long DEFAULT_BACK_OFF_VALUE = 1L;

  public ZTJStatusRecomputingSubmitted(String jobId) {
    this.jobId = jobId;
    this.maxConsumerBackoffBetweenRetriesDurationValue = DEFAULT_BACK_OFF_VALUE;
    this.maxConsumerDurationValue = MAX_CONSUMER_DURATION_VALUE;
    this.attemptNb = 0;
  }

  public ZTJStatusRecomputingSubmitted(
      String jobId, long maxConsumerBackoffBetweenRetriesDurationValue, int attemptNb) {
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
