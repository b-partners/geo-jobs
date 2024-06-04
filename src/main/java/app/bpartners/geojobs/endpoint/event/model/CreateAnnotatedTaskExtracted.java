package app.bpartners.geojobs.endpoint.event.model;

import app.bpartners.gen.annotator.endpoint.rest.model.CreateAnnotatedTask;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import javax.annotation.processing.Generated;
import lombok.*;

@Generated("EventBridge")
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@EqualsAndHashCode
@ToString
public class CreateAnnotatedTaskExtracted extends PojaEvent {
  @JsonProperty("annotationJobId")
  private String annotationJobId;

  @JsonProperty("createAnnotatedTask")
  private CreateAnnotatedTask createAnnotatedTask;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(10);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofMinutes(1);
  }
}
