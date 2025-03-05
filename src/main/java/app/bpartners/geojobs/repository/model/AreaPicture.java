package app.bpartners.geojobs.repository.model;

import static org.hibernate.type.SqlTypes.JSON;

import app.bpartners.geojobs.endpoint.rest.model.GeoPosition;
import app.bpartners.geojobs.endpoint.rest.model.OpenStreetMapLayer;
import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.endpoint.rest.model.TileInfo;
import app.bpartners.geojobs.endpoint.rest.model.Zoom;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;

@Entity
@Table(name = "area_picture")
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode
@ToString
public class AreaPicture implements Serializable {
  @Id private String id;
  private String address;

  @JdbcTypeCode(JSON)
  private Zoom zoom;

  private String fileKey;
  private boolean isExtended;
  @CreationTimestamp private Instant createdAt;
  private String communityId;

  @JdbcTypeCode(JSON)
  @Column(name = "current_tile", nullable = false)
  private TileInfo currentTile;

  @Transient private TileInfo referenceTile;

  @JdbcTypeCode(JSON)
  @Column(name = "geo_position")
  private GeoPosition geoPosition;

  @JdbcTypeCode(JSON)
  @Column(name = "available_layers")
  private List<OpenStreetMapLayer> availableLayers;

  public TileInfo getReferenceTile() {
    var currentCoordinates = currentTile.getCoordinates();
    return isExtended
        ? new TileInfo()
            .size(currentTile.getSize())
            .coordinates(
                new TileCoordinates()
                    .x(currentCoordinates.getX() - 3)
                    .y(currentCoordinates.getY() - 3)
                    .z(currentCoordinates.getZ()))
        : currentTile;
  }
}
