package app.bpartners.geojobs.service.lidar.model.geometry;

import java.util.ArrayList;
import java.util.List;

public record InclinedSurface(ArrayList<InclinedLine> lines) {
  public List<LasPointGeometry> points() {
    return lines.stream().map(InclinedLine::points).flatMap(List::stream).toList();
  }

  public boolean isLastLineMergeableWith(
      InclinedLine line, double epsilonX, double epsilonY, double epsilonZ, double epsilonSlope) {
    return lines.getLast().isMergeableWith(line, epsilonX, epsilonY, epsilonZ, epsilonSlope);
  }

  public void merge(InclinedLine inclinedLine) {
    lines.add(inclinedLine);
  }
}
