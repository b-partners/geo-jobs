package app.bpartners.geojobs.repository.model;

import static app.bpartners.geojobs.repository.model.Status.ProgressionStatus.PENDING;
import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.EAGER;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;

@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@Getter
@Setter
@ToString
@MappedSuperclass
public abstract class Job implements Serializable, Statusable<JobStatus> {
  @Id protected String id;
  protected String zoneName;
  protected String emailReceiver;
  @CreationTimestamp protected Instant submissionInstant;

  protected abstract JobType getType();

  @OneToMany(cascade = ALL, mappedBy = "jobId", fetch = EAGER)
  @Builder.Default
  private List<JobStatus> statusHistory = new ArrayList<>();

  public boolean isPending() {
    return PENDING.equals(getStatus().getProgression());
  }

  @Override
  public JobStatus from(Status status) {
    return JobStatus.from(id, status, getType());
  }

  @Override
  public void setStatusHistory(List<JobStatus> statusHistory) {
    if (statusHistory == null) {
      this.statusHistory = new ArrayList<>();
      return;
    }
    this.statusHistory = statusHistory;
  }
}
