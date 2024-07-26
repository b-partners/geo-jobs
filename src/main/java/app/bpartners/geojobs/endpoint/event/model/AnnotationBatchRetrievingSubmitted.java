package app.bpartners.geojobs.endpoint.event.model;

import app.bpartners.geojobs.endpoint.event.EventStack;
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
public class AnnotationBatchRetrievingSubmitted extends PojaEvent {
  @JsonProperty("jobId")
  private String jobId;

  @JsonProperty("annotationId")
  private String annotationJobId;

  @JsonProperty("annotationTaskId")
  private String annotationTaskId;

  @JsonProperty("imageSize")
  private int imageSize;

  @JsonProperty("xTile")
  private int xTile;

  @JsonProperty("yTile")
  private int yTile;

  @JsonProperty("zoom")
  private int zoom;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(5);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofMinutes(1);
  }

  @Override
  public EventStack getEventStack() {
    return EventStack.EVENT_STACK_2;
  }
}
