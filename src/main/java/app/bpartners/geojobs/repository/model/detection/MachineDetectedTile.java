package app.bpartners.geojobs.repository.model.detection;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.EAGER;
import static org.hibernate.type.SqlTypes.JSON;

import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.detection.RoofCovering;
import jakarta.persistence.*;
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

@Entity(name = "detected_tile")
@Table(name = "detected_tile")
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
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

  @Column(name = "primary_roof_covering_type")
  private RoofCoveringType primaryRoofCoveringType;

  @Column(name = "primary_roof_covering_area")
  private long primaryRoofCoveringArea;

  @Column(name = "secondary_roof_covering_type")
  private RoofCoveringType secondaryRoofCoveringType;

  @Column(name = "secondary_roof_covering_area")
  private long secondaryRoofCoveringArea;

  public String describe() {
    return "DetectedTile(id=" + id + ",tile=" + tile + "," + "jobId=" + zdjJobId + ")";
  }

  public void setPrimaryRoofCovering(RoofCovering covering) {
    primaryRoofCoveringArea = covering.area();
    primaryRoofCoveringType = covering.type();
  }

  public void setSecondaryRoofCovering(RoofCovering covering) {
    secondaryRoofCoveringArea = covering.area();
    secondaryRoofCoveringType = covering.type();
  }

  public RoofCovering getPrimaryRoofCovering() {
    return new RoofCovering(primaryRoofCoveringType, primaryRoofCoveringArea);
  }

  public RoofCovering getSecondaryRoofCovering() {
    return new RoofCovering(secondaryRoofCoveringType, secondaryRoofCoveringArea);
  }
}
