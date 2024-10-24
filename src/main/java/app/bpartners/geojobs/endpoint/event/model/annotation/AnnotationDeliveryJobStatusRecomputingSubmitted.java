package app.bpartners.geojobs.endpoint.event.model.annotation;

import static app.bpartners.geojobs.endpoint.event.EventStack.EVENT_STACK_2;

import app.bpartners.geojobs.endpoint.event.EventStack;
import app.bpartners.geojobs.endpoint.event.model.status.JobStatusRecomputingSubmitted;
import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@AllArgsConstructor
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class AnnotationDeliveryJobStatusRecomputingSubmitted extends JobStatusRecomputingSubmitted {
  private static final long MAX_CONSUMER_DURATION_IN_SECONDS = Duration.ofMinutes(5).getSeconds();
  public static final long INITIAL_BACKOFF_DURATION_IN_SECONDS = Duration.ofMinutes(2).getSeconds();

  public AnnotationDeliveryJobStatusRecomputingSubmitted(String jobId) {
    super(jobId, MAX_CONSUMER_DURATION_IN_SECONDS, INITIAL_BACKOFF_DURATION_IN_SECONDS);
  }

  @Override
  public EventStack getEventStack() {
    return EVENT_STACK_2;
  }
}
