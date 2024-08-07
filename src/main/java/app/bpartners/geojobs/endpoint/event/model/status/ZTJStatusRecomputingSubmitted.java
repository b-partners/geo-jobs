package app.bpartners.geojobs.endpoint.event.model.status;

import app.bpartners.geojobs.endpoint.event.EventStack;
import app.bpartners.geojobs.endpoint.rest.model.CreateFullDetection;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class ZTJStatusRecomputingSubmitted extends JobStatusRecomputingSubmitted {
  private static final long MAX_CONSUMER_DURATION_VALUE = 300L;
  private static final long DEFAULT_BACK_OFF_VALUE = 180L;
  private final CreateFullDetection createFullDetection;

  public ZTJStatusRecomputingSubmitted(String jobId, CreateFullDetection createFullDetection) {
    this.jobId = jobId;
    this.maxConsumerBackoffBetweenRetriesDurationValue = DEFAULT_BACK_OFF_VALUE;
    this.maxConsumerDurationValue = MAX_CONSUMER_DURATION_VALUE;
    this.createFullDetection = createFullDetection;
  }

  public ZTJStatusRecomputingSubmitted(String jobId) {
    this.jobId = jobId;
    this.maxConsumerBackoffBetweenRetriesDurationValue = DEFAULT_BACK_OFF_VALUE;
    this.maxConsumerDurationValue = MAX_CONSUMER_DURATION_VALUE;
    this.createFullDetection = new CreateFullDetection();
  }

  public ZTJStatusRecomputingSubmitted(
      String jobId, Long maxConsumerBackoffBetweenRetriesDurationValue) {
    super(jobId, MAX_CONSUMER_DURATION_VALUE, maxConsumerBackoffBetweenRetriesDurationValue);
    this.createFullDetection = new CreateFullDetection();
  }

  @Override
  public EventStack getEventStack() {
    return EventStack.EVENT_STACK_2;
  }
}
