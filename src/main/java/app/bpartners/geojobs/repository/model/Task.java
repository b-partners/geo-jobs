package app.bpartners.geojobs.repository.model;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.EAGER;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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

@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@Getter
@Setter
@ToString
@MappedSuperclass
@JsonIgnoreProperties({"status"})
public abstract class Task implements Serializable, Statusable<TaskStatus> {
  @Id private String id;

  private String jobId;
  @Getter @CreationTimestamp private Instant submissionInstant;

  @OneToMany(cascade = ALL, mappedBy = "taskId", fetch = EAGER)
  @Builder.Default
  private List<TaskStatus> statusHistory = new ArrayList<>();

  public abstract JobType getJobType();

  @Override
  public TaskStatus from(Status status) {
    return TaskStatus.from(id, status, getJobType());
  }

  @Override
  public void setStatusHistory(List<TaskStatus> statusHistory) {
    if (statusHistory == null) {
      this.statusHistory = new ArrayList<>();
      return;
    }
    this.statusHistory = statusHistory;
  }
}
