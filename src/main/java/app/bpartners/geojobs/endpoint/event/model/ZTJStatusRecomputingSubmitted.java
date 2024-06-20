package app.bpartners.geojobs.endpoint.event.model;

import static java.time.temporal.ChronoUnit.MINUTES;

import java.time.Duration;

import app.bpartners.geojobs.endpoint.event.EventStack;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@EqualsAndHashCode
@ToString
public class ZTJStatusRecomputingSubmitted extends PojaEvent {

  private String jobId;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.of(10, MINUTES);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.of(1, MINUTES);
  }

  @Override
  public EventStack getEventStack() {
    return EventStack.EVENT_STACK_2;
  }
}
