package app.bpartners.geojobs.endpoint.event.model;

import java.math.BigDecimal;
import java.time.Duration;
import lombok.Getter;
import org.locationtech.jts.geom.MultiPolygon;

@Getter
public class TileExtendedImageRequested extends PojaEvent {
  private final BigDecimal longitude;
  private final BigDecimal latitude;
  private final Integer zoom;
  private final String layer;
  private final MultiPolygon unifiedRoofMultiPolygon;

  public TileExtendedImageRequested(
      BigDecimal longitude,
      BigDecimal latitude,
      Integer zoom,
      String layer,
      MultiPolygon unifiedRoofMultiPolygon) {
    this.longitude = longitude;
    this.latitude = latitude;
    this.zoom = zoom;
    this.layer = layer;
    this.unifiedRoofMultiPolygon = unifiedRoofMultiPolygon;
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
