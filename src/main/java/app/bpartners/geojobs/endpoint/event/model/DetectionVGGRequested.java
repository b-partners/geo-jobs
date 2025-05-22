package app.bpartners.geojobs.endpoint.event.model;

import app.bpartners.geojobs.model.geometry.TiledPixelPolygonSerializable;
import java.time.Duration;
import java.util.List;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Getter
@EqualsAndHashCode(callSuper = false)
@ToString
public class DetectionVGGRequested extends PojaEvent {
  private String detectionId;
  private List<TiledPixelPolygonSerializable> filteredTiledPixelPolygons;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofSeconds(120);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofSeconds(60);
  }
}
