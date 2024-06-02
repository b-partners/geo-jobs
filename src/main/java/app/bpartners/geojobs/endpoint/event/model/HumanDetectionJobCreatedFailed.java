package app.bpartners.geojobs.endpoint.event.model;

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
  private String humanDetectionJobId;
  private int attemptNb;
}
