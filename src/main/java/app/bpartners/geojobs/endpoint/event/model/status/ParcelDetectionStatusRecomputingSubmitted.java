package app.bpartners.geojobs.endpoint.event.model.status;

import static app.bpartners.geojobs.endpoint.event.EventStack.EVENT_STACK_2;

import app.bpartners.geojobs.endpoint.event.EventStack;
import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@ToString
public class ParcelDetectionStatusRecomputingSubmitted extends JobStatusRecomputingSubmitted {
  private static final long MAX_CONSUMER_DURATION_IN_SECONDS = Duration.ofMinutes(2).toSeconds();
  public static final long INITIAL_BACKOFF_IN_SECONDS = Duration.ofMinutes(2).toSeconds();

  public ParcelDetectionStatusRecomputingSubmitted(String parcelDetectionJobId) {
    super(parcelDetectionJobId, MAX_CONSUMER_DURATION_IN_SECONDS, INITIAL_BACKOFF_IN_SECONDS);
  }

  @Override
  public EventStack getEventStack() {
    return EVENT_STACK_2;
  }
}