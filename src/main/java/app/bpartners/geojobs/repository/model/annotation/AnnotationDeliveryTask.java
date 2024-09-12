package app.bpartners.geojobs.repository.model.annotation;

import app.bpartners.geojobs.job.model.JobType;
import app.bpartners.geojobs.job.model.Task;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "annotation_retrieving_task")
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Data
@EqualsAndHashCode(callSuper = false)
public class AnnotationDeliveryTask extends Task {
  private String annotationTaskId;
  private String annotationJobId;
  private String humanZoneDetectionJobId;

  @Override
  public JobType getJobType() {
    return null;
  }

  @Override
  public Task semanticClone() {
    return null;
  }
}
