package app.bpartners.geojobs.endpoint.event.model;

import app.bpartners.geojobs.repository.model.detection.ParcelDetectionJob;
import java.time.Duration;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@EqualsAndHashCode(callSuper = true)
@ToString
public class ParcelDetectionJobCreated extends PojaEvent {
  private String zdjId;
  private ParcelDetectionJob parcelDetectionJob;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(5L);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofMinutes(1L);
  }
}
