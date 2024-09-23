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
// TODO: rename full_detection to detection and detection to machine_detection
@Table(name = "full_detection")
public class Detection implements Serializable {
  @Id private String id;
  private String endToEndId;

  @Column(name = "geojson_s3_file_key")
  private String geojsonS3FileKey;

  private String shapeFileKey;

  @Column(name = "zdj_id")
  private String zdjId;

  @Column(name = "ztj_id")
  private String ztjId;

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
  private DetectionOverallConfiguration detectionOverallConfiguration;

  @JdbcTypeCode(JSON)
  private List<Feature> geoJsonZone;

  public CreateMachineDetection getCreateMachineDetection() {
    CreateMachineDetection createMachineDetection = new CreateMachineDetection();
    if (bpToitureModel != null) {
      createMachineDetection.setActualInstance(bpToitureModel);
    } else if (bpLomModel != null) {
      createMachineDetection.setActualInstance(bpLomModel);
    } else {
      return null;
    }
    return createMachineDetection;
  }
}
