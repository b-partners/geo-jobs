package app.bpartners.geojobs.model.lidar.planes.exporter;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Plane3DExtractionStep {
  INIT_POINTS(0),
  RAW_PLANE_EXTRACTION(1),
  PLANE_CONTINUITY_EXTRACTION(2),
  CLOSED_PLANE_MERGING(3),
  RAW_DELIMITATION_EXTRACTION(4),
  DELIMITATION_SIMPLIFICATION(5);

  private final int stepIndex;

  public String toFilePrefix() {
    return String.format("Step%d_%s", stepIndex, this);
  }
}
