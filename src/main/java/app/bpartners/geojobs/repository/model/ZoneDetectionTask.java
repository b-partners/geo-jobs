package app.bpartners.geojobs.repository.model;

import static app.bpartners.geojobs.repository.model.JobStatus.JobType.DETECTION;
import static java.util.stream.Collectors.toList;
import static javax.persistence.CascadeType.ALL;

import app.bpartners.geojobs.repository.model.types.PostgresTypes;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

@TypeDef(name = PostgresTypes.JSONB, typeClass = JsonBinaryType.class)
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@JsonIgnoreProperties({"status"})
public class ZoneDetectionTask implements Serializable {
  @Id private String id;

  private String jobId;
  @Getter @CreationTimestamp private Instant submissionInstant;

  @OneToMany(cascade = ALL, mappedBy = "taskId")
  private List<TaskStatus> statusHistory = new ArrayList<>();

  @Type(type = PostgresTypes.JSONB)
  @Column(columnDefinition = PostgresTypes.JSONB)
  private Tile tile;

  public void addStatus(TaskStatus status) {
    statusHistory.add(status);
  }

  public TaskStatus getStatus() {
    return TaskStatus.from(
        id, Status.reduce(statusHistory.stream().map(status -> (Status) status).collect(toList())), DETECTION);
  }
}
