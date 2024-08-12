package app.bpartners.geojobs.endpoint.event.model;

import java.time.Duration;
import javax.annotation.processing.Generated;

import app.bpartners.geojobs.endpoint.event.EventStack;
import lombok.*;

import static app.bpartners.geojobs.endpoint.event.EventStack.EVENT_STACK_2;

@Generated("EventBridge")
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@EqualsAndHashCode
@ToString
public class JobAnnotationProcessed extends PojaEvent {
  private String jobId;
  private Double minConfidence;
  private String annotationJobWithObjectsIdTruePositive;
  private String annotationJobWithObjectsIdFalsePositive;
  private String annotationJobWithoutObjectsId;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(10L);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofMinutes(1L);
  }

  @Override
  public EventStack getEventStack() {
    return EVENT_STACK_2;
  }
}
