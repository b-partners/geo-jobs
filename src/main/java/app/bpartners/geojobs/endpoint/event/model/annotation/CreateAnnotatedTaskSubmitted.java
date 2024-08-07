package app.bpartners.geojobs.endpoint.event.model.annotation;

import app.bpartners.gen.annotator.endpoint.rest.model.CreateAnnotatedTask;
import app.bpartners.geojobs.endpoint.event.EventStack;
import app.bpartners.geojobs.endpoint.event.model.PojaEvent;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import javax.annotation.processing.Generated;
import lombok.*;

import static app.bpartners.geojobs.endpoint.event.EventStack.EVENT_STACK_2;

@Generated("EventBridge")
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@EqualsAndHashCode
@ToString
public class CreateAnnotatedTaskSubmitted extends PojaEvent {
  @JsonProperty("annotationJobId")
  private String annotationJobId;

  @JsonProperty("createAnnotatedTask")
  private CreateAnnotatedTask createAnnotatedTask;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(3);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofMinutes(1);
  }

  @Override
  public EventStack getEventStack() {
    return EVENT_STACK_2;
  }
}
