package app.bpartners.geojobs.repository.model.geo.detection;

import static app.bpartners.geojobs.repository.model.geo.GeoJobType.DETECTION;
import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.EnumType.STRING;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import app.bpartners.geojobs.repository.model.Job;
import app.bpartners.geojobs.repository.model.JobType;
import app.bpartners.geojobs.repository.model.geo.tiling.ZoneTilingJob;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;

@Entity
@Getter
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ZoneDetectionJob extends Job {
  @OneToOne(cascade = ALL)
  private ZoneTilingJob zoneTilingJob;

  @Enumerated(STRING)
  @JdbcTypeCode(NAMED_ENUM)
  private DetectionType detectionType;

  @Override
  protected JobType getType() {
    return DETECTION;
  }

  public enum DetectionType {
    MACHINE,
    HUMAN
  }
}
