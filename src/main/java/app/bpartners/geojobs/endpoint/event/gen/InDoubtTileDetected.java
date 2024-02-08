package app.bpartners.geojobs.endpoint.event.gen;

import app.bpartners.geojobs.repository.model.geo.detection.DetectedTile;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;
import lombok.*;

@Generated("EventBridge")
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@EqualsAndHashCode
@ToString
public class InDoubtTileDetected {
  @JsonProperty("detectedTile")
  private DetectedTile tile;
}
