package app.bpartners.geojobs.repository.model.detection;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.EAGER;
import static org.hibernate.type.SqlTypes.JSON;

import app.bpartners.geojobs.repository.model.tiling.Tile;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
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
@Table(name = "detected_tile")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class MachineDetectedTile implements Serializable {
  @Id private String id;

  @JdbcTypeCode(JSON)
  private Tile tile;

  @CreationTimestamp private Instant creationDatetime;

  @OneToMany(cascade = ALL, fetch = EAGER, mappedBy = "detectedTileId")
  private List<DetectedObject> detectedObjects;

  private String bucketPath;

  private String parcelId;
  private String parcelJobId;
  private String zdjJobId;

  @Column(name = "human_detection_job_id")
  private String humanDetectionJobId;

  public String describe() {
    return "DetectedTile(id=" + id + ",tile=" + tile + "," + "jobId=" + zdjJobId + ")";
  }

  public DetectedObject getFirstObject() {
    try {
      return detectedObjects == null ? null : detectedObjects.getFirst();
    } catch (NoSuchElementException e) {
      return null;
    }
  }
}
