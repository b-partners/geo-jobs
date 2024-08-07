package app.bpartners.geojobs.endpoint.event.model.annotation;

import static app.bpartners.geojobs.endpoint.event.EventStack.EVENT_STACK_2;

import app.bpartners.geojobs.endpoint.event.EventStack;
import app.bpartners.geojobs.endpoint.event.model.PojaEvent;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
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
@EqualsAndHashCode(callSuper = false)
@ToString
public class AnnotationTaskRetrievingSubmitted extends PojaEvent {
  @JsonProperty("humanZdjId")
  private String humanZdjId;

  @JsonProperty("annotationRetrievingJobId")
  private String annotationRetrievingJobId;

  @JsonProperty("annotationJobId")
  private String annotationJobId;

  @JsonProperty("imageWidth")
  private int imageWidth;

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofMinutes(1);
  }

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(10);
  }

  @Override
  public EventStack getEventStack() {
    return EVENT_STACK_2;
  }
}
