package app.bpartners.geojobs.repository.model;

import static org.hibernate.type.SqlTypes.JSON;

import app.bpartners.geojobs.endpoint.rest.model.AreaPictureImageSource;
import app.bpartners.geojobs.endpoint.rest.model.Zoom;
import app.bpartners.geojobs.endpoint.rest.model.ZoomLevel;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;

@Entity
@Table(name = "area_picture_map_layer")
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode
@ToString
public class AreaPictureMapLayer implements Serializable, Comparable<AreaPictureMapLayer> {
  @Id private String id;

  @JdbcTypeCode(JSON)
  private AreaPictureImageSource source;

  private int year;
  private String name;
  private String departmentName;

  @JdbcTypeCode(JSON)
  private ZoomLevel maxZoomLevel;

  private int precisionLevelInCm;

  @Override
  public int compareTo(AreaPictureMapLayer o2) {
    var precisionComparison = Integer.compare(o2.precisionLevelInCm, this.precisionLevelInCm);
    if (precisionComparison == 0) {
      return Integer.compare(this.year, o2.year);
    }
    return precisionComparison;
  }

  public Zoom getMaxZoom() {
    return new Zoom().level(maxZoomLevel).number(ArcgisImageZoom.from(maxZoomLevel));
  }
}
