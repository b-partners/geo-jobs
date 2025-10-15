package app.bpartners.geojobs.service.lidar.model.geometry;

import app.bpartners.geojobs.service.lidar.preprocessing.Grouper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static app.bpartners.geojobs.service.lidar.model.geometry.Axis.Y;

public record InclinedPlane(ArrayList<InclinedLine> lines) {
  public List<LasPointGeometry> points() {
    var points = lines.stream().map(InclinedLine::points).flatMap(List::stream).toList();
    return cleaned(points);
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

  private List<LasPointGeometry> cleaned(Collection<LasPointGeometry> points){
    var grouper = new Grouper(Y, 0.5);
    var cleaned = grouper.apply(points);

    return cleaned
        .stream()
        .filter(p -> p.size() > 20)
        .flatMap(List::stream)
        .toList();
  }
}
