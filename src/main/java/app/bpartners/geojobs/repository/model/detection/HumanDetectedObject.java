package app.bpartners.geojobs.repository.model.detection;

import static org.hibernate.type.SqlTypes.JSON;

import app.bpartners.gen.annotator.endpoint.rest.model.Label;
import app.bpartners.gen.annotator.endpoint.rest.model.Polygon;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;

@Entity
@Table(name = "human_detected_object")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class HumanDetectedObject implements Serializable {
  @Id private String id;

  @JdbcTypeCode(JSON)
  private Polygon feature;

  @JoinColumn(referencedColumnName = "id")
  private String humanDetectedTileId;

  @JdbcTypeCode(JSON)
  private Label label;

  private String confidence;
}