package app.bpartners.geojobs.endpoint.event.model;

import app.bpartners.geojobs.endpoint.rest.model.GeoServerParameter;
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
public class ImportedZoneTilingJobSaved extends PojaEvent {
  private Long startFrom;
  private Long endAt;
  private String jobId;
  private String bucketName;
  private String bucketPathPrefix;
  private GeoServerParameter geoServerParameter;
  private String geoServerUrl;

  @Override
  public Duration maxDuration() {
    return Duration.ofMinutes(10);
  }

  @Override
  public Duration maxBackoffBetweenRetries() {
    return Duration.ofMinutes(1);
  }
}
