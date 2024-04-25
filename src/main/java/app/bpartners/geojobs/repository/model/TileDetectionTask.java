package app.bpartners.geojobs.repository.model;

import app.bpartners.geojobs.repository.model.tiling.Tile;
import lombok.*;

@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class TileDetectionTask {
  private String taskId;
  private String parcelId;
  private String jobId;
  private Tile tile;
}
