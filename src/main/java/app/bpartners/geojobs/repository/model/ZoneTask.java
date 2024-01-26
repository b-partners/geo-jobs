package app.bpartners.geojobs.repository.model;

import static app.bpartners.geojobs.repository.model.JobStatus.JobType.TILING;
import static java.util.stream.Collectors.toList;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.FetchType;
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
public class ZoneTask implements Serializable {
  @Id private String id;

  private String jobId;
  @Getter @CreationTimestamp private Instant submissionInstant;

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "taskId", fetch = FetchType.EAGER)
  private List<TaskStatus> statusHistory = new ArrayList<>();

  public TaskStatus getStatus() {
    return TaskStatus.from(
        id,
        Status.reduce(statusHistory.stream().map(status -> (Status) status).collect(toList())),
        TILING);
  }

  public void addStatus(TaskStatus status) {
    statusHistory.add(status);
  }
}
