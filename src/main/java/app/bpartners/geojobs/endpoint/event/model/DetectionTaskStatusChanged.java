package app.bpartners.geojobs.endpoint.event.model;

import app.bpartners.geojobs.repository.model.detection.ParcelDetectionTask;
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
public class DetectionTaskStatusChanged extends PojaEvent {
  @JsonProperty("oldTask")
  private ParcelDetectionTask oldTask;

  @JsonProperty("newTask")
  private ParcelDetectionTask newTask;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(1);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofMinutes(1);
  }
}
