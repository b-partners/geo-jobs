package app.bpartners.geojobs.repository.model;

import static app.bpartners.geojobs.repository.model.Status.ProgressionStatus.PENDING;
import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.EAGER;
import static org.hibernate.annotations.FetchMode.SELECT;

import app.bpartners.geojobs.repository.model.geo.JobType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Fetch;

@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@Getter
@Setter
@ToString
@MappedSuperclass
public abstract class Job<T extends Task> implements Serializable {
  @Id protected String id;
  protected String zoneName;
  protected String emailReceiver;
  @CreationTimestamp protected Instant submissionInstant;

  // note(LazyInitializationException): thrown when fetch type is LAZY, hence using EAGER
  @OneToMany(cascade = ALL, mappedBy = "jobId", fetch = EAGER)
  @Fetch(SELECT)
  protected List<JobStatus> statusHistory;

  // note(LazyInitializationException)
  @OneToMany(mappedBy = "jobId", cascade = ALL, fetch = EAGER)
  @Fetch(SELECT)
  protected List<T> tasks = new ArrayList<>();

  public void refreshStatusHistory() {
    statusHistory.add(getStatus());
  }

  public JobStatus getStatus() {
    return JobStatus.from(
        id,
        Status.reduce(tasks.stream().map(Task::getStatus).map(status -> (Status) status).toList()),
        getJobType());
  }

  public boolean isPending() {
    return PENDING.equals(getStatus().getProgression());
  }

  public abstract JobType getJobType();
}
