package app.bpartners.geojobs.endpoint.event.gen;

import app.bpartners.geojobs.repository.model.TileDetectionTask;
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
public class TileDetectionTaskSucceeded {
  @JsonProperty("tileDetectionTask")
  private TileDetectionTask tileDetectionTask;
}
