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

  // TODO(1M-task.statuses): if there are 1M statuses, this will NOT do! Instead:
  //  1. Rm job.tasks and retrieve only paginated tasks per job as needed (and make it LAZY!)
  //  2. Do not rely on full knowledge of task.statuses to compute job.status, instead:
  //     2.1. When task is finished:
  //          2.1.1. If failed and retry.max NOT reached:
  //                  2.1.1.1 Let persisted task.status remain processing (and NOT failed)
  //                  2.1.1.2 Just retry (actually do nothing and wait for visibility timeout)
  //          2.1.2. If failed and retry.max reached:
  //                 Reduce task.status with the current job.statusHistory persisted in db
  //                 /!\ Mind status.creationDatetime for both task and job
  //                     Indeed task.statuses might arrive in any possible order
  //                 Note that job.statusHistory is updated by a comparison to ONLY ONE task.status
  // at a time,
  //                 In particular, it is NEVER computed by loading 1M task.statuses all at the same
  // time
  //          2.1.3. If succeeded: same as 2.1.2
  //     2.2. If persisted job status changed, then send JobStatusChanged (as before)

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
