package app.bpartners.geojobs.repository.model;

import app.bpartners.geojobs.endpoint.rest.model.Geometry;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@Data
@ToString
@EqualsAndHashCode
public class Feature implements Serializable {
  private String id;
  private Integer zoom;

  private FeatureGeometry geometry;

  @NoArgsConstructor
  @AllArgsConstructor
  @Builder(toBuilder = true)
  @Data
  @ToString
  @EqualsAndHashCode
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class FeatureGeometry implements Serializable {
    private Geometry.TypeEnum geometryType;
    private String actualInstanceStringValue;
  }
}
