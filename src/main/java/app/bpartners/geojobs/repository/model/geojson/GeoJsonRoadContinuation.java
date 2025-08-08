package app.bpartners.geojobs.repository.model.geojson;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "road_continuation")
@Getter
@Setter
public class GeoJsonRoadContinuation {
  @Id
  @Column(name = "rc_id")
  private String id;

  @Column(name = "original_geojson_path")
  private String originalGeoJsonPath;

  @Column(name = "continued_geojson_path")
  private String continuedGeoJsonPath;

  @Enumerated(EnumType.STRING)
  @Column(columnDefinition = "process_status")
  private RoadContinuationProcessStatus status;
}
