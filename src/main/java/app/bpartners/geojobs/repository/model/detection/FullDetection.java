package app.bpartners.geojobs.repository.model.detection;

import static org.hibernate.type.SqlTypes.JSON;

import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectConfiguration;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
@Getter
@Setter
@EqualsAndHashCode
@Table(name = "full_detection")
public class FullDetection {
  @Id private String id;
  private String endToEndId;
  private String geoJsonS3FileKey;
  private String zDJId;
  private String zTJId;

  @JdbcTypeCode(JSON)
  private DetectableObjectConfiguration detectableObjectConfiguration;
}
