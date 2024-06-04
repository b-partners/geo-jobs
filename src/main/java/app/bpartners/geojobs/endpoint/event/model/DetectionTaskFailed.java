package app.bpartners.geojobs.endpoint.event.model;

import app.bpartners.geojobs.repository.model.detection.DetectionTask;
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
public class DetectionTaskFailed extends PojaEvent {
  private DetectionTask task;
  private int attemptNb;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(1);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofMinutes(1);
  }
}
