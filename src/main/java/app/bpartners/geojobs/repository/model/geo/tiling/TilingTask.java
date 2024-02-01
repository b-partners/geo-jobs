package app.bpartners.geojobs.repository.model.geo.tiling;

import static app.bpartners.geojobs.repository.model.geo.JobType.TILING;
import static org.hibernate.type.SqlTypes.JSON;

import app.bpartners.geojobs.repository.model.Task;
import app.bpartners.geojobs.repository.model.geo.JobType;
import app.bpartners.geojobs.repository.model.geo.Parcel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Entity;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Getter
@Setter
@ToString
@JsonIgnoreProperties({"status"})
public class TilingTask extends Task implements Serializable {
  @JdbcTypeCode(JSON)
  private Parcel parcel;

  @Override
  public JobType getJobType() {
    return TILING;
  }
}
