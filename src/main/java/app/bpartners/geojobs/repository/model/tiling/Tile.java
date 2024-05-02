package app.bpartners.geojobs.repository.model.tiling;

import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import java.io.Serializable;
import java.time.Instant;
import lombok.*;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class Tile implements Serializable {
  private String id;
  private Instant creationDatetime;
  private TileCoordinates coordinates;
  private String bucketPath;

  @Override
  public String toString() {
    return "Tile{"
        + "id='"
        + id
        + '\''
        + ", creationDatetime="
        + creationDatetime
        + ", coordinates="
        + coordinates
        + ", bucketPath='"
        + bucketPath
        + '\''
        + '}';
  }
}
