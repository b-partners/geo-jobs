package app.bpartners.geojobs.repository.model.tiling;

import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import java.io.Serializable;
import java.time.Instant;
import lombok.*;

@Builder
@ToString
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

  public Tile duplicate(String tileId) {
    return Tile.builder()
        .id(tileId)
        .creationDatetime(this.creationDatetime)
        .coordinates(this.coordinates)
        .bucketPath(this.bucketPath)
        .build();
  }
}
