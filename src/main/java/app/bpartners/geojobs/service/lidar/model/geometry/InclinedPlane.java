package app.bpartners.geojobs.service.lidar.model.geometry;

import java.util.ArrayList;
import java.util.List;

public record InclinedPlane(ArrayList<InclinedLine> lines) {
  public List<LasPointGeometry> points() {
    return lines.stream().map(InclinedLine::points).flatMap(List::stream).toList();
  }

  public double slope() {
    return lines.stream().mapToDouble(InclinedLine::slope).average().orElse(0);
  }

  public boolean isCompatibleWith(
      InclinedLine line, double dx, double dy, double dz, double epsilonSlope) {
    return lines.getLast().isCompatibleWith(line, dx, dy, dz, epsilonSlope);
  }

  public void merge(InclinedLine inclinedLine) {
    lines.add(inclinedLine);
  }
}
