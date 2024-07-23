package app.bpartners.geojobs.endpoint.event.model;

import app.bpartners.geojobs.endpoint.event.EventStack;
import app.bpartners.geojobs.endpoint.rest.model.CreateFullDetection;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode
@ToString
public class ZTJStatusRecomputingSubmitted extends JobStatusRecomputingSubmitted {
  private static final long MAX_CONSUMER_DURATION_VALUE = 300L;
  private static final long DEFAULT_BACK_OFF_VALUE = 180L;
  private CreateFullDetection createFullDetection;

  public ZTJStatusRecomputingSubmitted(String jobId, CreateFullDetection createFullDetection) {
    this.jobId = jobId;
    this.maxConsumerBackoffBetweenRetriesDurationValue = DEFAULT_BACK_OFF_VALUE;
    this.maxConsumerDurationValue = MAX_CONSUMER_DURATION_VALUE;
    this.attemptNb = 0;
    this.createFullDetection = createFullDetection;
  }

  public ZTJStatusRecomputingSubmitted(String jobId) {
    this.jobId = jobId;
    this.maxConsumerBackoffBetweenRetriesDurationValue = DEFAULT_BACK_OFF_VALUE;
    this.maxConsumerDurationValue = MAX_CONSUMER_DURATION_VALUE;
    this.attemptNb = 0;
  }

  public ZTJStatusRecomputingSubmitted(
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
