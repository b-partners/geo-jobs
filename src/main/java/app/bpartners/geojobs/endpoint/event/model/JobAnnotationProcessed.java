package app.bpartners.geojobs.endpoint.event.model;

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
  private String annotationJobWithObjectsIdTruePositive;
  private String annotationJobWithObjectsIdFalsePositive;
  private String annotationJobWithoutObjectsId;

  @Override
  public Duration maxDuration() {
    return Duration.ofMinutes(10L);
  }

  @Override
  public Duration maxBackoffBetweenRetries() {
    return Duration.ofMinutes(1L);
  }
}
