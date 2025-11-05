package app.bpartners.geojobs.endpoint.event.model;

import java.time.Duration;

import app.bpartners.geojobs.endpoint.event.EventStack;
import lombok.*;

import static app.bpartners.geojobs.endpoint.event.EventStack.EVENT_STACK_4;

@AllArgsConstructor
@Data
@EqualsAndHashCode(callSuper = false)
@Builder(toBuilder = true)
public class CityJSONRequestCreated extends PojaEvent {
  private final String requestId;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(20);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofMinutes(2);
  }

  @Override
  public EventStack getEventStack() {
    return EVENT_STACK_4;
  }
}
