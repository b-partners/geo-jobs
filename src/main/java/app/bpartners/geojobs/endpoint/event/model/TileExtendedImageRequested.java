package app.bpartners.geojobs.endpoint.event.model;

import java.math.BigDecimal;
import java.time.Duration;
import lombok.Getter;

@Getter
public class TileExtendedImageRequested extends PojaEvent {
  private final BigDecimal longitude;
  private final BigDecimal latitude;
  private final Integer zoom;
  private final String layer;

  public TileExtendedImageRequested(
      BigDecimal longitude, BigDecimal latitude, Integer zoom, String layer) {
    this.longitude = longitude;
    this.latitude = latitude;
    this.zoom = zoom;
    this.layer = layer;
  }

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofSeconds(30);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofSeconds(30);
  }
}
