package app.bpartners.geojobs.endpoint.event.gen;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Generated("EventBridge")
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@EqualsAndHashCode
@ToString
public class ZoneDetectionJobSucceeded {
  @JsonProperty("jobId")
  private String jobId;
}
