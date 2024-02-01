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
    statusHistory.add(status);
  }
}
