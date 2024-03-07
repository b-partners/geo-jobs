package app.bpartners.geojobs.endpoint.event.gen;

import app.bpartners.geojobs.repository.model.detection.HumanDetectionJob;
import javax.annotation.processing.Generated;
import lombok.*;

@Generated("EventBridge")
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@EqualsAndHashCode
@ToString
public class HumanDetectionJobCreatedFailed {
  private HumanDetectionJob humanDetectionJob; // TODO: send ID so detected tiles are not sent here
  private int attemptNb;
}
