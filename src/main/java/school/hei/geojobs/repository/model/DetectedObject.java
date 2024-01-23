package school.hei.geojobs.repository.model;

import static javax.persistence.CascadeType.ALL;
import static org.hibernate.annotations.FetchMode.SELECT;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import java.io.Serializable;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import school.hei.geojobs.endpoint.rest.model.Feature;
import school.hei.geojobs.repository.model.types.PostgresTypes;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@EqualsAndHashCode
@ToString
@TypeDef(name = PostgresTypes.JSONB, typeClass = JsonBinaryType.class)
public class DetectedObject implements Serializable {
  @Id private String id;

  @Type(type = PostgresTypes.JSONB)
  @Column(columnDefinition = PostgresTypes.JSONB)
  private Feature feature;

  @JoinColumn(referencedColumnName = "id")
  private String detectedTileId;

  @OneToMany(cascade = ALL, mappedBy = "objectId")
  @Fetch(SELECT)
  private List<DetectableObjectType> detectedObjectTypes;
}
