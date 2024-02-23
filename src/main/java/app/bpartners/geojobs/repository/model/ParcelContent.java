package app.bpartners.geojobs.repository.model;

import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.GeoServerParameter;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties({"tilingStatus"})
public class ParcelContent implements Serializable {
  private String id;
  private Feature feature;
  private URL geoServerUrl;
  private GeoServerParameter geoServerParameter;

  @Builder.Default private List<Tile> tiles = new ArrayList<>();

  private Status tilingStatus;
  private Instant creationDatetime;

  public void setTiles(List<Tile> tiles) {
    if (tiles == null) {
      this.tiles = new ArrayList<>();
      return;
    }
    this.tiles = tiles;
  }
}
