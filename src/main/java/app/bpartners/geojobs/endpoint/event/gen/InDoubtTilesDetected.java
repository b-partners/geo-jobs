package app.bpartners.geojobs.endpoint.event.gen;

import app.bpartners.geojobs.repository.model.detection.DetectedTile;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;
import lombok.*;

@Generated("EventBridge")
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@EqualsAndHashCode
@ToString
public class InDoubtTilesDetected {
  @JsonProperty("jobId")
  private String jobId;
}
