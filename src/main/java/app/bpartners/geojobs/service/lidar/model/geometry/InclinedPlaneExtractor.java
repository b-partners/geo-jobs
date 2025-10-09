package app.bpartners.geojobs.service.lidar.model.geometry;

import static app.bpartners.geojobs.service.lidar.model.geometry.Axis.X;
import static java.util.stream.Collectors.toList;

import app.bpartners.geojobs.service.lidar.preprocessing.Aligner;
import app.bpartners.geojobs.service.lidar.preprocessing.DuplicatePointsOnTwoAxesCleaner;
import app.bpartners.geojobs.service.lidar.preprocessing.Grouper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public class InclinedPlaneExtractor
    implements Function<Collection<LasPointGeometry>, Collection<InclinedPlane>> {
  public static final int DEFAULT_MINIMUM_LINE_POINT_COUNT = 2;
  public static final int DEFAULT_MINIMUM_PLANE_POINT_COUNT = 30;

  public static final double DEFAULT_LINE_POINT_DY = 1;
  public static final double DEFAULT_LINE_POINT_DZ = 0.25;

  public static final double DEFAULT_LINE_MERGE_DX = 4;
  public static final double DEFAULT_LINE_MERGE_DY = 3;
  public static final double DEFAULT_LINE_MERGE_DZ = 0.5;
  public static final double DEFAULT_LINE_MERGE_EPSILON_SLOPE = 10;

  private final InclinedPlaneExtractorConf conf;

  private final Aligner alignerByX;
  private final Grouper grouperByX;
  private final DuplicatePointsOnTwoAxesCleaner duplicatePointsOnZYCleaner;

  public InclinedPlaneExtractor(InclinedPlaneExtractorConf conf) {
    this.conf = conf;
    this.alignerByX = new Aligner(X);
    this.grouperByX = new Grouper(X, 1);
    this.duplicatePointsOnZYCleaner = DuplicatePointsOnTwoAxesCleaner.zyKeepFirst(0.3, 0.3);
  }

  public InclinedPlaneExtractor() {
    this(
        new InclinedPlaneExtractorConf(
            DEFAULT_MINIMUM_LINE_POINT_COUNT,
            DEFAULT_MINIMUM_PLANE_POINT_COUNT,
            DEFAULT_LINE_POINT_DY,
            DEFAULT_LINE_POINT_DZ,
            DEFAULT_LINE_MERGE_DX,
            DEFAULT_LINE_MERGE_DY,
            DEFAULT_LINE_MERGE_DZ,
            DEFAULT_LINE_MERGE_EPSILON_SLOPE));
  }

  @Override
  public List<InclinedPlane> apply(Collection<LasPointGeometry> points) {
    var groupedByX = grouperByX.apply(points);
    var cleanedGroupedByX =
        groupedByX.stream()
            .map(
                group -> {
                  var aligned = alignerByX.apply(group).stream().toList();
                  return duplicatePointsOnZYCleaner.apply(aligned).stream().toList();
                })
            .collect(toList());

    var inclinedLines = toGroupedInlinedLines(cleanedGroupedByX);
    var planes = mergeCloseInclinedLines(inclinedLines);

    return planes.stream()
        .filter(surface -> surface.points().size() > conf.minimumPlanePointCount())
        .toList();
  }

  public List<InclinedPlane> mergeCloseInclinedLines(
      List<List<InclinedLine>> groupedInclinedLines) {
    List<InclinedPlane> results = new ArrayList<>();
    List<InclinedLine> used = new ArrayList<>();

    for (int i = 0; i < groupedInclinedLines.size(); i++) {
      var group = groupedInclinedLines.get(i);

      for (var line : group) {
        if (line.points().size() < conf.minimumLinePointCount()) {
          continue;
        }

        if (used.contains(line)) {
          continue;
        }

        var currentSurface = new InclinedPlane(new ArrayList<>(List.of(line)));
        for (int j = i + 1; j < groupedInclinedLines.size(); j++) {
          for (var otherLine : groupedInclinedLines.get(j)) {
            if (used.contains(otherLine)) {
              continue;
            }

            if (otherLine.points().size() < conf.minimumLinePointCount()) {
              continue;
            }

            if (currentSurface.isCompatibleWith(
                otherLine,
                conf.lineMergeToleranceX(),
                conf.lineMergeToleranceY(),
                conf.lineMergeToleranceZ(),
                conf.lineMergeToleranceSlope())) {
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

  private List<List<InclinedLine>> toGroupedInlinedLines(
      List<List<LasPointGeometry>> lasPointGroupedByX) {
    return lasPointGroupedByX.stream()
        .map(
            points ->
                InclinedLine.from(points, conf.pointLineDy(), conf.pointLineDz()).stream().toList())
        .toList();
  }

  public record InclinedPlaneExtractorConf(
      int minimumLinePointCount,
      int minimumPlanePointCount,
      double pointLineDy,
      double pointLineDz,
      double lineMergeToleranceX,
      double lineMergeToleranceY,
      double lineMergeToleranceZ,
      double lineMergeToleranceSlope) {}
}
