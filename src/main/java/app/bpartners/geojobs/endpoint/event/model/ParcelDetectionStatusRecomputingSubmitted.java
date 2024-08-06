package app.bpartners.geojobs.endpoint.event.model;

import app.bpartners.geojobs.endpoint.event.EventStack;
import lombok.*;

@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class ParcelDetectionStatusRecomputingSubmitted extends JobStatusRecomputingSubmitted {
  private static final long MAX_CONSUMER_DURATION_VALUE = 100L;
  public static final long DEFAULT_BACKOFF_VALUE = 100L;

  public ParcelDetectionStatusRecomputingSubmitted(String parcelDetectionJobId) {
    this.jobId = parcelDetectionJobId;
    this.maxConsumerBackoffBetweenRetriesDurationValue = DEFAULT_BACKOFF_VALUE;
    this.maxConsumerDurationValue = MAX_CONSUMER_DURATION_VALUE;
  }

  @Override
  public EventStack getEventStack() {
    return EventStack.EVENT_STACK_2;
  }
}
