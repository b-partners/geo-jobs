package app.bpartners.geojobs.repository.model;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.EAGER;
import static java.util.stream.Collectors.toList;

import app.bpartners.geojobs.repository.model.geo.JobType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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

@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@Getter
@Setter
@ToString
@MappedSuperclass
@JsonIgnoreProperties({"status"})
public abstract class Task implements Serializable {
  @Id private String id;

  private String jobId;
  @Getter @CreationTimestamp private Instant submissionInstant;

  @OneToMany(cascade = ALL, mappedBy = "taskId", fetch = EAGER)
  private List<TaskStatus> statusHistory = new ArrayList<>();

  public TaskStatus getStatus() {
    return TaskStatus.from(
        id,
        Status.reduce(statusHistory.stream().map(status -> (Status) status).collect(toList())),
        getJobType());
  }

  public abstract JobType getJobType();

  public void addStatus(TaskStatus status) {
    // TODO(invalid-history): change to current(or set? or
    // update?)Status(progression,health,_now()_) and check sanity (by comparing to
    // oldStatus=getStatus()) before adding it to statusHistory
    statusHistory.add(status);
    // TODO(invalid-history):
    // https://eu-west-3.console.aws.amazon.com/cloudwatch/home?region=eu-west-3#logsV2:log-groups/log-group/$252Faws$252Flambda$252Fpreprod-compute-geo-jobs-WorkerFunction-UB0GNzuahUcv/log-events/2024$252F02$252F07$252F$255B38$255Da243edf56b59432eb12a16383db699eb$3Fstart$3D1707340881941$26refEventId$3D38074973977430869773683008094709067084776606311260422148
  }
}
