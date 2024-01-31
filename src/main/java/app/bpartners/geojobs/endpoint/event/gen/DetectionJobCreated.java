package app.bpartners.geojobs.endpoint.event.gen;

import app.bpartners.geojobs.repository.model.geo.tiling.TilingTask;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import javax.annotation.processing.Generated;
import lombok.*;

@Generated("EventBridge")
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode
@Data
@ToString
public class DetectionJobCreated implements Serializable {
  @JsonProperty("tilingTask")
  private TilingTask tilingTask;
}
