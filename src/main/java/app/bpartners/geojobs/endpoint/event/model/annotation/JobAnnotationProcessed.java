package app.bpartners.geojobs.endpoint.event.model.annotation;

import app.bpartners.geojobs.endpoint.event.model.PojaEvent;
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
}
