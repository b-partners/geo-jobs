package app.bpartners.geojobs.endpoint.event.gen;

import app.bpartners.geojobs.endpoint.rest.model.GeoServerParameter;
import javax.annotation.processing.Generated;
import lombok.*;

@Generated("EventBridge")
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@EqualsAndHashCode
@ToString
public class ImportedZoneTilingJobSaved {
  private String jobId;
  private String bucketPathKey;
  private GeoServerParameter geoServerParameter;
  private String geoServerUrl;
}
