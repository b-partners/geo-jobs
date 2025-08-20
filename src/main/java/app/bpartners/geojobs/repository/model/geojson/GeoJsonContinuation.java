package app.bpartners.geojobs.repository.model.geojson;

import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import app.bpartners.geojobs.job.model.Status;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;

@Entity
@Table(name = "geo_json_continuation")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
public class GeoJsonContinuation {
  @Id
  @Column(nullable = false)
  private String id;

  @Column(name = "file_key")
  private String fileyKey;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(NAMED_ENUM)
  private Status.ProgressionStatus status;
}
