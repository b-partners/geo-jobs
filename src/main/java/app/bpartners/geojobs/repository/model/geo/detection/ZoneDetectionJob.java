package app.bpartners.geojobs.repository.model.geo.detection;

import static app.bpartners.geojobs.repository.model.geo.JobType.DETECTION;
import static jakarta.persistence.CascadeType.ALL;

import app.bpartners.geojobs.repository.model.Job;
import app.bpartners.geojobs.repository.model.geo.JobType;
import app.bpartners.geojobs.repository.model.geo.tiling.ZoneTilingJob;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ZoneDetectionJob extends Job<DetectionTask> implements Serializable {
  @OneToOne(cascade = ALL)
  private ZoneTilingJob zoneTilingJob;

  @Override
  public JobType getJobType() {
    return DETECTION;
  }
}
