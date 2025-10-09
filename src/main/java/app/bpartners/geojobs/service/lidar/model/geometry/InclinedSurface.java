package app.bpartners.geojobs.service.lidar.model.geometry;

import java.util.ArrayList;
import java.util.List;

public record InclinedSurface(ArrayList<InclinedLine> lines) {
  public List<LasPointGeometry> points() {
    return lines
        .stream()
        .map(InclinedLine::points)
        .flatMap(List::stream)
        .toList();
  }

  public InclinedLine.ZVariation variation(double epsilonZ){
      return lines
          .getFirst()
          .variation(epsilonZ);
  }

  public double slope(){
     return lines
         .stream()
         .mapToDouble(InclinedLine::slope)
         .average()
         .orElse(0);
  }

  public boolean isLastLineMergeableWith(
      InclinedLine line, double epsilonX, double epsilonY, double epsilonZ, double epsilonSlope) {
    return lines
        .getLast()
        .isMergeableWith(line, epsilonX, epsilonY, epsilonZ, epsilonSlope);
  }

  public void merge(InclinedLine inclinedLine) {
    lines.add(inclinedLine);
  }
}
