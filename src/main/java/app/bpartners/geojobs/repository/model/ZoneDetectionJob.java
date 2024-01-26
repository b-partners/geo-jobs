package app.bpartners.geojobs.repository.model;

import static app.bpartners.geojobs.repository.model.JobStatus.JobType.DETECTION;
import static java.util.stream.Collectors.toList;
import static javax.persistence.CascadeType.ALL;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.OneToOne;
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
public class ZoneDetectionJob extends ZoneJob<ZoneDetectionTask> implements Serializable {
  @OneToOne(cascade = ALL)
  private ZoneTilingJob zoneTilingJob;

  public JobStatus getStatus() {
    return JobStatus.from(
        getId(),
        Status.reduce(getStatusHistory().stream().map(status -> (Status) status).collect(toList())),
        DETECTION);
  }

  public void addStatus(JobStatus status) {
    getStatusHistory().add(status);
  }
}
