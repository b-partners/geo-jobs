package app.bpartners.geojobs.repository.model.detection;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.*;

@Entity
@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class DetectionStep {
  @Id private String id;

  private DetectionStepName name;

  @OneToOne(mappedBy = "step")
  private Detection detection;

  @OneToOne
  @JoinColumn(referencedColumnName = "id", name = "status_id")
  private Status status;
}
