package app.bpartners.geojobs.endpoint.event.model;

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
public class TileDetectionTaskCreatedFailed {
  @JsonProperty("tileDetectionTask")
  private TileDetectionTaskCreated tileDetectionTaskCreated;

  @JsonProperty("attemptNb")
  private int attemptNb;
}
