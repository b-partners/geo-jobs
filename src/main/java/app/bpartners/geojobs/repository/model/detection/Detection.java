package app.bpartners.geojobs.repository.model.detection;

import static org.hibernate.type.SqlTypes.JSON;

import app.bpartners.geojobs.endpoint.rest.model.*;
import jakarta.persistence.*;
import java.io.Serializable;
import java.util.List;
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
@Builder(toBuilder = true)
@Getter
@Setter
@EqualsAndHashCode
@Table(name = "detection")
public class Detection implements Serializable {
  @Id private String id;
  private String endToEndId;

  @Column(name = "geojson_s3_file_key")
  private String geojsonS3FileKey;

  private String shapeFileKey;
  private String excelFileKey;

  @Column(name = "zdj_id")
  private String zdjId;

  @Column(name = "ztj_id")
  private String ztjId;

  private String zoneName;

  private String emailReceiver;

  @JoinColumn(referencedColumnName = "id", name = "community_owner_id")
  private String communityOwnerId;

  // TODO: save as entity as it map now the domain detectableObject
  @JdbcTypeCode(JSON)
  private List<DetectableObjectConfiguration> detectableObjectConfigurations;

  @JdbcTypeCode(JSON)
  private BPToitureModel bpToitureModel;

  @JdbcTypeCode(JSON)
  private BPLomModel bpLomModel;

  // TODO: save as entity
  @JdbcTypeCode(JSON)
  private GeoServerProperties geoServerProperties;

  @JdbcTypeCode(JSON)
  private List<Feature> geoJsonZone;

  public DetectableObjectModel getDetectableObjectModel() {
    DetectableObjectModel detectableObjectModel = new DetectableObjectModel();
    if (bpToitureModel != null) {
      detectableObjectModel.setActualInstance(bpToitureModel);
    } else if (bpLomModel != null) {
      detectableObjectModel.setActualInstance(bpLomModel);
    } else {
      return null;
    }
    return detectableObjectModel;
  }
}
