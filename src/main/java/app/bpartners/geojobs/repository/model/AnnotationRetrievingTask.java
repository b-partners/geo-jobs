package app.bpartners.geojobs.repository.model;

import app.bpartners.geojobs.job.model.JobType;
import app.bpartners.geojobs.job.model.Task;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import static app.bpartners.geojobs.repository.model.GeoJobType.DETECTION;

@Entity
@Table(name = "annotation_retrieving_task")
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Data
@EqualsAndHashCode(callSuper = false)
public class AnnotationRetrievingTask extends Task {
  private String annotationTaskId;

  @Override
  public JobType getJobType() {
    return DETECTION;
  }

  @Override
  public Task semanticClone() {
    return null;
  }
}
