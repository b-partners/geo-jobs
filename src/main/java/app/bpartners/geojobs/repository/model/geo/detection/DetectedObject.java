package app.bpartners.geojobs.repository.model.geo.detection;

import static jakarta.persistence.CascadeType.ALL;
import static org.hibernate.annotations.FetchMode.SELECT;
import static org.hibernate.type.SqlTypes.JSON;

import app.bpartners.geojobs.endpoint.rest.model.Feature;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.JdbcTypeCode;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class DetectedObject implements Serializable {
  @Id private String id;

  @JdbcTypeCode(JSON)
  private Feature feature;

  @JoinColumn(referencedColumnName = "id")
  private String detectedTileId;

  @OneToMany(cascade = ALL, mappedBy = "objectId")
  @Fetch(SELECT)
  private List<DetectableObjectType> detectedObjectTypes;

  private Double confidence;
}
