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
  private final String detectionIdentifier;

  public TileExtendedImageRequested(
      BigDecimal longitude,
      BigDecimal latitude,
      Integer zoom,
      String layer,
      String detectionIdentifier) {
    this.longitude = longitude;
    this.latitude = latitude;
    this.zoom = zoom;
    this.layer = layer;
    this.detectionIdentifier = detectionIdentifier;
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
