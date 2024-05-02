package app.bpartners.geojobs.endpoint.event.gen;

import app.bpartners.geojobs.repository.model.detection.DetectionTask;
import javax.annotation.processing.Generated;
import lombok.*;

@Generated("EventBridge")
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@EqualsAndHashCode
@ToString
// TODO: delete because now detectionTask status is computed by tileDetectionTasks statuses
public class DetectionTaskSucceeded {
  private DetectionTask task;
}
