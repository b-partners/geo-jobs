package app.bpartners.geojobs.endpoint.event.model;

import app.bpartners.geojobs.repository.model.geojson.GeoJsonConversionTask;
import java.time.Duration;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class GeoJsonConversionTaskSucceeded extends PojaEvent {
  private GeoJsonConversionTask geoJsonConversionTask;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(1);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofSeconds(15);
  }
}
