package app.bpartners.geojobs.endpoint.event.model;

import app.bpartners.geojobs.endpoint.event.EventStack;
import lombok.*;

@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class ParcelDetectionStatusRecomputingSubmitted extends JobStatusRecomputingSubmitted {
  private static final long MAX_CONSUMER_DURATION_VALUE = 300L;
  public static final long DEFAULT_BACKOFF_VALUE = 180L;

  public ParcelDetectionStatusRecomputingSubmitted(String parcelDetectionJobId) {
    this.jobId = parcelDetectionJobId;
    this.maxConsumerBackoffBetweenRetriesDurationValue = DEFAULT_BACKOFF_VALUE;
    this.maxConsumerDurationValue = MAX_CONSUMER_DURATION_VALUE;
    this.attemptNb = 0;
  }

  public ParcelDetectionStatusRecomputingSubmitted(
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
