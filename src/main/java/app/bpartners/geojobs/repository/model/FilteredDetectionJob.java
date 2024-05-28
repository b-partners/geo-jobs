package app.bpartners.geojobs.repository.model;

import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import lombok.*;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
public class FilteredDetectionJob {
  private String initialJobId;
  private ZoneDetectionJob succeededJob;
  private ZoneDetectionJob notSucceededJob;
}
