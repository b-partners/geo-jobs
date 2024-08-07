package app.bpartners.geojobs.endpoint.event.model.status;

import static app.bpartners.geojobs.endpoint.event.EventStack.EVENT_STACK_2;

import app.bpartners.geojobs.endpoint.event.EventStack;
import app.bpartners.geojobs.endpoint.rest.model.CreateFullDetection;
import java.time.Duration;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class ZTJStatusRecomputingSubmitted extends JobStatusRecomputingSubmitted {
  private static final long MAX_CONSUMER_DURATION_IN_SECONDS = Duration.ofMinutes(5).toSeconds();
  public static final long INITIAL_BACKOFF_IN_SECONDS = Duration.ofMinutes(3).toSeconds();

  // TODO: Lou doesn't understand this attribute
  private final CreateFullDetection createFullDetection;

  public ZTJStatusRecomputingSubmitted(String jobId, CreateFullDetection createFullDetection) {
    super(jobId, MAX_CONSUMER_DURATION_IN_SECONDS, INITIAL_BACKOFF_IN_SECONDS);
    this.createFullDetection = createFullDetection;
  }

  public ZTJStatusRecomputingSubmitted(String jobId) {
    super(jobId, MAX_CONSUMER_DURATION_IN_SECONDS, INITIAL_BACKOFF_IN_SECONDS);
    this.createFullDetection = new CreateFullDetection();
  }

  public ZTJStatusRecomputingSubmitted(
      String jobId, Long maxConsumerBackoffBetweenRetriesDurationValue) {
    super(jobId, MAX_CONSUMER_DURATION_IN_SECONDS, maxConsumerBackoffBetweenRetriesDurationValue);
    this.createFullDetection = new CreateFullDetection();
  }

  @Override
  public EventStack getEventStack() {
    return EVENT_STACK_2;
  }
}
