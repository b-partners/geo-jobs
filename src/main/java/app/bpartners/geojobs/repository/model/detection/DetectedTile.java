package app.bpartners.geojobs.repository.model.detection;

import static jakarta.persistence.CascadeType.ALL;
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

  private String parcelId;

  private String jobId;

  @Column(name = "human_detection_job_id")
  private String humanDetectionJobId;

  public String describe() {
    String detectedObject =
        getFirstObject() == null ? null : getFirstObject().getDetectableObjectType().toString();
    Double confidence = getFirstObject() == null ? null : getFirstObject().getComputedConfidence();
    return "DetectedTile(id="
        + id
        + ",detectedObject="
        + detectedObject
        + ","
        + "confidence="
        + confidence
        + ","
        + "jobId="
        + jobId
        + ")";
  }

  public DetectedObject getFirstObject() {
    try {
      return detectedObjects == null ? null : detectedObjects.getFirst();
    } catch (NoSuchElementException e) {
      return null;
    }
  }
}
