package app.bpartners.geojobs.repository.model.detection;

import static jakarta.persistence.FetchType.EAGER;

import jakarta.persistence.*;
import java.util.List;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class HumanDetectionJob {
  @Id private String id;
  private String annotationJobId;

  @JoinColumn(referencedColumnName = "id")
  private String zoneDetectionJobId;

  @OneToMany(fetch = EAGER)
  @JoinColumn(name = "human_detection_job_id")
  private List<DetectedTile> detectedTiles;

  // TODO: must be persisted but can be retrieved by zoneDetectionJobId
  @Transient private List<DetectableObjectConfiguration> detectableObjectConfigurations;
}
