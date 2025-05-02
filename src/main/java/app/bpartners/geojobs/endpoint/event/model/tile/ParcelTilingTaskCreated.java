package app.bpartners.geojobs.endpoint.event.model.tile;

import app.bpartners.geojobs.endpoint.event.model.TaskCreated;
import app.bpartners.geojobs.repository.model.tiling.ParcelTilingTask;
import lombok.*;

@Getter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ToString
public class ParcelTilingTaskCreated extends TaskCreated<ParcelTilingTask> {
  public ParcelTilingTaskCreated(ParcelTilingTask task) {
    super(task);
  }
}
