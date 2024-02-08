package app.bpartners.geojobs.repository.model.geo;

import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.GeoServerParameter;
import app.bpartners.geojobs.repository.model.Status;
import app.bpartners.geojobs.repository.model.geo.tiling.Tile;
import java.io.Serializable;
import java.net.URL;
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
public class Parcel implements Serializable {
  private String id;
  private Feature feature;
  private URL geoServerUrl;
  private GeoServerParameter geoServerParameter;
  private List<Tile> tiles = new ArrayList<>();
  private Status tilingStatus;
  private String creationDatetime;
}
