package app.bpartners.geojobs.endpoint.event.model;

import app.bpartners.geojobs.repository.model.detection.DetectionTask;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import javax.annotation.processing.Generated;
import lombok.*;

@Generated("EventBridge")
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@EqualsAndHashCode
@ToString
public class DetectionTaskStatusChanged implements Serializable {
  @JsonProperty("oldTask")
  private DetectionTask oldTask;

  @JsonProperty("newTask")
  private DetectionTask newTask;
}
