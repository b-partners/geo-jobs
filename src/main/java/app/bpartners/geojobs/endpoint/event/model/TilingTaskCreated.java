package app.bpartners.geojobs.endpoint.event.model;

import app.bpartners.geojobs.repository.model.tiling.TilingTask;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
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
public class TilingTaskCreated implements Serializable {
  @JsonProperty("tilingTask")
  private TilingTask task;
}
