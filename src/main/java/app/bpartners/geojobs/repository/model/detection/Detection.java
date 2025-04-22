package app.bpartners.geojobs.repository.model.detection;

import static org.hibernate.type.SqlTypes.JSON;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper;
import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.repository.model.Feature;
import jakarta.persistence.*;
import java.io.Serializable;
import java.util.List;
import lombok.*;
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
  private String imageFileKey;

  @Column(name = "zdj_id")
  private String zdjId;

  @Column(name = "ztj_id")
  private String ztjId;

  private String zoneName;

  private String emailReceiver;
  private boolean isRooferMade;

  @JoinColumn(referencedColumnName = "id", name = "community_owner_id")
  private String communityOwnerId;

  // TODO: save as entity as it map now the domain detectableObject
  @JdbcTypeCode(JSON)
  private List<DetectableObjectConfiguration> detectableObjectConfigurations;

  @JdbcTypeCode(JSON)
  private BPToitureModel bpToitureModel;

  @JdbcTypeCode(JSON)
  private BPLomModel bpLomModel;

  @JdbcTypeCode(JSON)
  private BPZanModel bpZanModel;

  // TODO: save as entity
  @JdbcTypeCode(JSON)
  private GeoServerProperties geoServerProperties;

  @Column(name = "geo_json_zone")
  @JdbcTypeCode(JSON)
  @Getter(AccessLevel.NONE)
  private List<Feature> providedGeoJsonZone;

  @JdbcTypeCode(JSON)
  @Getter(AccessLevel.NONE)
  private List<Feature> multiPolygonGeoJsonZone;

  @JdbcTypeCode(JSON)
  private List<String> convertedAddresses;

  public List<app.bpartners.geojobs.endpoint.rest.model.Feature> getProvidedGeoJsonZone() {
    return providedGeoJsonZone == null
        ? null
        : providedGeoJsonZone.stream().map(FeatureMapper::toRestFeature).toList();
  }

  public List<app.bpartners.geojobs.endpoint.rest.model.Feature> getMultiPolygonGeoJsonZone() {
    return multiPolygonGeoJsonZone == null
        ? null
        : multiPolygonGeoJsonZone.stream().map(FeatureMapper::toRestFeature).toList();
  }

  public DetectableObjectModel getDetectableObjectModel() {
    DetectableObjectModel detectableObjectModel = new DetectableObjectModel();
    if (bpToitureModel != null) {
      detectableObjectModel.setActualInstance(bpToitureModel);
    } else if (bpLomModel != null) {
      detectableObjectModel.setActualInstance(bpLomModel);
    } else if (bpZanModel != null) {
      detectableObjectModel.setActualInstance(bpZanModel);
    } else {
      return null;
    }
    return detectableObjectModel;
  }

  public boolean isSucceeded() {
    return getGeojsonS3FileKey() != null;
  }

  public boolean isStillOnConfiguringStep() {
    return getMultiPolygonGeoJsonZone() == null
        || getMultiPolygonGeoJsonZone().isEmpty()
        || getGeoServerProperties() == null;
  }

  public boolean isStillOnTilingStep() {
    return getZdjId() == null;
  }

  public boolean isTilingPending() {
    return getZtjId() == null && !isStillOnConfiguringStep();
  }

  public boolean isMachineDetectionStepProcessing(ZoneDetectionJob zoneDetectionJob) {
    return !zoneDetectionJob.isFinished();
  }

  private boolean isMachineDetectionFinished(ZoneDetectionJob zoneDetectionJob) {
    return zoneDetectionJob.isFinished();
  }

  public boolean isHumanDetectionStepProcessing(ZoneDetectionJob zoneDetectionJob) {
    return isMachineDetectionFinished(zoneDetectionJob) && geojsonS3FileKey == null;
  }
}
