package app.bpartners.geojobs.repository.model.geo.detection;

import static app.bpartners.geojobs.repository.model.geo.JobType.DETECTION;
import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.EnumType.STRING;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import app.bpartners.geojobs.repository.model.Job;
import app.bpartners.geojobs.repository.model.geo.JobType;
import app.bpartners.geojobs.repository.model.geo.tiling.ZoneTilingJob;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToOne;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;

@Entity
@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ZoneDetectionJob extends Job<DetectionTask> implements Serializable {
  @OneToOne(cascade = ALL)
  private ZoneTilingJob zoneTilingJob;

  @Enumerated(STRING)
  @JdbcTypeCode(NAMED_ENUM)
  private DetectionType type;

  public enum DetectionType {
    MACHINE,
    HUMAN
  }

  @Override
  public JobType getJobType() {
    return DETECTION;
  }
}
