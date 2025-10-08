package app.bpartners.geojobs.service.lidar.model.geometry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public record InclinedSurfaceSeparator(InclinedSurfaceSeparatorConf conf)
    implements Function<Collection<LasPointGeometry>, List<InclinedSurface>> {

  public InclinedSurfaceSeparator() {
    this(new InclinedSurfaceSeparatorConf());
  }

  @Override
  public List<InclinedSurface> apply(Collection<LasPointGeometry> points) {
    var groupedByX = LasPointGroupedByX.from(points, conf.epsilonX());
    var inclinedLines = toGroupedInlinedLines(groupedByX);

    // TODO: remove
    if (false) {
      return inclinedLines.stream()
          .flatMap(List::stream)
          .map(l -> new InclinedSurface(new ArrayList<>(List.of(l))))
          .toList();
    }

    return mergeCloseInclinedLines(inclinedLines);
  }

  public List<InclinedSurface> mergeCloseInclinedLines(
      List<List<InclinedLine>> groupedInclinedLines) {
    List<InclinedSurface> results = new ArrayList<>();
    List<InclinedLine> used = new ArrayList<>();

    for (int i = 0; i < groupedInclinedLines.size(); i++) {
      var group = groupedInclinedLines.get(i);

      for (var line : group) {
        if (line.points().size() < conf.minimumPointCount()) {
          continue;
        }

        if (used.contains(line)) {
          continue;
        }

        var currentSurface = new InclinedSurface(new ArrayList<>(List.of(line)));
        for (int j = i + 1; j < groupedInclinedLines.size(); j++) {
          for (var otherLine : groupedInclinedLines.get(j)) {
            if (used.contains(otherLine)) {
              continue;
            }

            if (otherLine.points().size() < conf.minimumPointCount()) {
              continue;
            }

            if (currentSurface.isLastLineMergeableWith(
                otherLine,
                8 /* Const value */,
                conf.epsilonY(),
                conf.epsilonZ(),
                conf.epsilonSlope())) {
              used.add(otherLine);
              currentSurface.merge(otherLine);
            }
          }
        }

        results.add(currentSurface);
        used.add(line);
      }
    }

    return results;
  }

  private List<List<InclinedLine>> toGroupedInlinedLines(LasPointGroupedByX lasPointGroupedByX) {
    return lasPointGroupedByX.groups().stream()
        .map(points -> InclinedLine.from(points, conf.epsilonY(), conf.epsilonZ()))
        .toList();
  }

  public record InclinedSurfaceSeparatorConf(
      int minimumPointCount,
      double epsilonX,
      double epsilonY,
      double epsilonZ,
      double epsilonSlope) {
    public static int DEFAULT_MINIMUM_POINT_COUNT = 2;
    public static double DEFAULT_EPSILON_Y = 2;
    public static double DEFAULT_EPSILON_Z = 1;
    public static double DEFAULT_EPSILON_X = 0.7;
    public static double DEFAULT_EPSILON_SLOPE = 5;

    public InclinedSurfaceSeparatorConf() {
      this(
          DEFAULT_MINIMUM_POINT_COUNT,
          DEFAULT_EPSILON_X,
          DEFAULT_EPSILON_Y,
          DEFAULT_EPSILON_Z,
          DEFAULT_EPSILON_SLOPE);
    }
  }
}
