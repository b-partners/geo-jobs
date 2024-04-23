package app.bpartners.geojobs.repository.model.detection;

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
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class DetectedObject implements Serializable {
  @Id private String id;

  @JdbcTypeCode(JSON)
  private Feature feature;

  @JoinColumn(referencedColumnName = "id")
  private String detectedTileId;

  @OneToMany(cascade = ALL, mappedBy = "objectId")
  @Fetch(SELECT)
  private List<DetectableObjectType> detectedObjectTypes;

  private Double computedConfidence;

  public boolean isInDoubt(List<DetectableObjectConfiguration> objectConfigurations) {
    DetectableType detectableObjectType = getDetectableObjectType();
    Optional<DetectableObjectConfiguration> optionalConfiguration =
        objectConfigurations.stream()
            .filter(
                detectableObjectConfiguration ->
                    detectableObjectConfiguration.getObjectType().equals(detectableObjectType))
            .findFirst();
    return optionalConfiguration.isPresent()
        && optionalConfiguration.get().getConfidence() != null
        && computedConfidence != null
        && optionalConfiguration.get().getConfidence() > computedConfidence;
  }

  public DetectableType getDetectableObjectType() {
    if (detectedObjectTypes.isEmpty()) {
      return null;
    }
    int detectableObjectsSize = detectedObjectTypes.size();
    DetectableObjectType firstObjectType = detectedObjectTypes.get(0);
    if (detectableObjectsSize > 1) {
      log.info(
          "Detectable objects for detected object is "
              + detectableObjectsSize
              + "("
              + detectedObjectTypes
              + ") but only 1 ("
              + firstObjectType
              + ") chosen");
    }
    return firstObjectType.getDetectableType();
  }
}
