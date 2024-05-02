package app.bpartners.geojobs.repository.model;

import static app.bpartners.geojobs.repository.model.GeoJobType.DETECTION;
import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.EAGER;
import static org.hibernate.type.SqlTypes.JSON;

import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.job.model.Statusable;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;

@Entity
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@JsonIgnoreProperties({"tilingStatus"}) // TODO: must not be here
public class TileDetectionTask implements Statusable<TaskStatus> {
  @Id private String id;
  private String detectionTaskId;
  private String parcelId;
  private String jobId;

  @JdbcTypeCode(JSON)
  private Tile tile;

  @OneToMany(cascade = ALL, mappedBy = "taskId", fetch = EAGER)
  @Builder.Default
  private List<TaskStatus> statusHistory = new ArrayList<>();

  @Getter @CreationTimestamp private Instant submissionInstant;

  public TileDetectionTask(
      String id,
      String detectionTaskId,
      String parcelId,
      String jobId,
      Tile tile,
      List<TaskStatus> statusHistory) {
    this.id = id;
    this.detectionTaskId = detectionTaskId;
    this.parcelId = parcelId;
    this.jobId = jobId;
    this.tile = tile;
    this.statusHistory = statusHistory;
  }

  @Override
  public TaskStatus from(Status status) {
    return TaskStatus.from(id, status, DETECTION);
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
