package app.bpartners.geojobs.repository.model;

import static app.bpartners.geojobs.repository.model.GeoJobType.DETECTION;
import static java.time.Instant.now;
import static org.hibernate.type.SqlTypes.JSON;

import app.bpartners.geojobs.job.model.*;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;

@Entity
@SuperBuilder(toBuilder = true)
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@JsonIgnoreProperties({"tilingStatus"}) // TODO: must not be here
public class TileDetectionTask extends Task implements Serializable {
  @Id private String id;
  private String parcelId;

  @JdbcTypeCode(JSON)
  private Tile tile;

  public TileDetectionTask(
      String id,
      String asJobId,
      String parcelId,
      String jobId,
      Tile tile,
      List<TaskStatus> statusHistory) {
    super(id, jobId, asJobId, now(), statusHistory);
    this.id = id;
    this.parcelId = parcelId;
    this.tile = tile;
  }

  @Override
  public JobType getJobType() {
    return DETECTION;
  }

  @Override
  public Task semanticClone() {
    return this.toBuilder().statusHistory(new ArrayList<>(getStatusHistory())).build();
  }
}
