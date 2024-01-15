package school.hei.geotiler.repository.model.geo;

import java.io.Serializable;
import java.net.URL;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import school.hei.geotiler.endpoint.rest.model.Feature;
import school.hei.geotiler.endpoint.rest.model.GeoServerParameter;
import school.hei.geotiler.repository.model.Status;
import school.hei.geotiler.repository.model.Tile;

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
  private List<Tile> tiles;
  private Status tilingStatus;
}
