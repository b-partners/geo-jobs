package app.bpartners.geojobs.repository.model.tiling;

import static app.bpartners.geojobs.repository.model.GeoJobType.TILING;

import app.bpartners.geojobs.job.model.Job;
import app.bpartners.geojobs.job.model.JobType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Entity;
import java.util.ArrayList;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@JsonIgnoreProperties({"status", "done"})
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Setter
public class ZoneTilingJob extends Job {
  @Override
  protected JobType getType() {
    return TILING;
  }

  protected String endToEndId;

  @Override
  public Job semanticClone() {
    return this.toBuilder().statusHistory(new ArrayList<>(getStatusHistory())).build();
  }

  public ZoneTilingJob duplicate(String jobId) {
    return ZoneTilingJob.builder()
        .id(jobId)
        .zoneName(this.zoneName)
        .emailReceiver(this.emailReceiver)
        .statusHistory(this.getStatusHistory())
        .submissionInstant(this.getSubmissionInstant())
        .build();
  }

  public String describe() {
    return "ZoneTilingJob{"
        + "id='"
        + id
        + '\''
        + ", zoneName='"
        + zoneName
        + '\''
        + ", submissionInstant="
        + submissionInstant
        + '}';
  }
}
