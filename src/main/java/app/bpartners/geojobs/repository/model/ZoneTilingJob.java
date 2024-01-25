package app.bpartners.geojobs.repository.model;

import static app.bpartners.geojobs.repository.model.JobStatus.JobType.TILING;
import static java.util.stream.Collectors.toList;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import javax.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@SuperBuilder
@AllArgsConstructor
@ToString
@JsonIgnoreProperties({"status", "done"})
public class ZoneTilingJob extends AbstractZoneJob<ZoneTilingTask> implements Serializable {

  public JobStatus getStatus() {
    return JobStatus.from(
        this.getId(),
        Status.reduce(
            this.getStatusHistory().stream().map(status -> (Status) status).collect(toList())),
        TILING);
  }

  public void addStatus(JobStatus status) {
    this.getStatusHistory().add(status);
  }
}
