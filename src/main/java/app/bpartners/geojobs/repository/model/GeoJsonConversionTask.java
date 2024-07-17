package app.bpartners.geojobs.repository.model;

import static app.bpartners.geojobs.repository.model.GeoJobType.DETECTION;

import app.bpartners.geojobs.job.model.JobType;
import app.bpartners.geojobs.job.model.Task;
import jakarta.persistence.Entity;
import java.util.ArrayList;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Entity
@SuperBuilder(toBuilder = true)
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class GeoJsonConversionTask extends Task {
  private String geoJsonUrl;

  @Override
  public JobType getJobType() {
    return DETECTION;
  }

  @Override
  public Task semanticClone() {
    return this.toBuilder().statusHistory(new ArrayList<>(getStatusHistory())).build();
  }
}
