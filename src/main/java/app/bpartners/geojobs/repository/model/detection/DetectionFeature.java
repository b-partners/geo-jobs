package app.bpartners.geojobs.repository.model.detection;

import static jakarta.persistence.EnumType.STRING;
import static org.hibernate.type.SqlTypes.JSON;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import app.bpartners.geojobs.repository.model.Feature;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import java.time.Instant;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
public class DetectionFeature {
  @Id private String id;

  private String idFeature;

  private String idDetection;

  @Enumerated(STRING)
  @JdbcTypeCode(NAMED_ENUM)
  private DetectionFeatureType detectionFeatureType;

  @JdbcTypeCode(JSON)
  private Feature feature;

  private Instant creationDatetime;
}
