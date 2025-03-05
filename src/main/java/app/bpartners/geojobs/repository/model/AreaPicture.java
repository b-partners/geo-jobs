package app.bpartners.geojobs.repository.model;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
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
import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;

@Entity
@Table(name = "area_picture")
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
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

  public Point getGeoPositionAsPoint() {
    return geometryFactory.createPoint(
        new Coordinate(geoPosition.getLatitude(), geoPosition.getLongitude()));
  }
}
