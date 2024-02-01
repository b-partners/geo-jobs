package app.bpartners.geojobs.repository.model.geo.detection;

import static jakarta.persistence.CascadeType.ALL;
import static org.hibernate.type.SqlTypes.JSON;

import app.bpartners.geojobs.repository.model.geo.tiling.Tile;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class DetectedTile implements Serializable {
  @Id private String id;

  @JdbcTypeCode(JSON)
  private Tile tile;

  @CreationTimestamp private Instant creationDatetime;

  @OneToMany(cascade = ALL, mappedBy = "detectedTileId")
  private List<DetectedObject> detectedObjects;

  private String bucketPath;
}
