package app.bpartners.geojobs.repository.model.detection;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "detection_step")
public class DetectionStep {
  @Id private String id;

  private DetectionStepName name;

  private Status.Progression progression;

  private Status.Health health;

  @Column(name = "creation_datetime")
  private Instant creationDatetime;

  @Column(name = "detection_id")
  private String detectionId;
}
