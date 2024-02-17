package app.bpartners.geojobs.endpoint.event.gen;

import app.bpartners.geojobs.repository.model.tiling.TilingTask;
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
public class TilingTaskSucceeded {
  private TilingTask task;
}
