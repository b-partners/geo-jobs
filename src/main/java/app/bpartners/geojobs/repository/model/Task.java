package app.bpartners.geojobs.repository.model;

import static java.util.stream.Collectors.toList;
import static javax.persistence.CascadeType.ALL;
import static javax.persistence.FetchType.EAGER;

import app.bpartners.geojobs.repository.model.geo.JobType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;
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
    statusHistory.add(status);
  }
}