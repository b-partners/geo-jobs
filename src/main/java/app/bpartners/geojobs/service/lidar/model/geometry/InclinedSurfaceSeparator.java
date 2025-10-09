package app.bpartners.geojobs.service.lidar.model.geometry;

import app.bpartners.geojobs.service.lidar.preprocessing.Aligner;
import app.bpartners.geojobs.service.lidar.preprocessing.DuplicatePointsOnTwoAxesCleaner;
import app.bpartners.geojobs.service.lidar.preprocessing.Grouper;
import app.bpartners.geojobs.test.Main;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import static app.bpartners.geojobs.service.lidar.model.geometry.Axis.X;
import static java.util.stream.Collectors.toList;

public class InclinedSurfaceSeparator implements Function<Collection<LasPointGeometry>, Collection<InclinedSurface>> {
    public static final int DEFAULT_MINIMUM_POINT_COUNT = 2;
    public static final double DEFAULT_EPSILON_X = 1;
    public static final double DEFAULT_EPSILON_Y = 3;
    public static final double DEFAULT_EPSILON_Z = 0.05;
    public static final double DEFAULT_EPSILON_SLOPE = 10;

    private final InclinedSurfaceSeparatorConf conf;

    private final Aligner alignerByX;
    private final Grouper grouperByX;
    private final DuplicatePointsOnTwoAxesCleaner duplicatePointsOnZYCleaner;

    public InclinedSurfaceSeparator() {
        this.conf = new InclinedSurfaceSeparatorConf();
        this.alignerByX = new Aligner(X);
        this.grouperByX = new Grouper(X, 0.5);
        this.duplicatePointsOnZYCleaner = DuplicatePointsOnTwoAxesCleaner.zyKeepFirst(0.3, 0.2);
    }

    @Override
    public Collection<InclinedSurface> apply(Collection<LasPointGeometry> points) {
        var groupedByX = grouperByX.apply(points);
        var cleanedGroupedByX = groupedByX.stream().map(group -> {
            var aligned = alignerByX.apply(group);
            return duplicatePointsOnZYCleaner.apply(aligned).stream().toList();
        }).collect(toList());

        var inclinedLines = toGroupedInlinedLines(cleanedGroupedByX);
        // TODO: remove
        if (true) {
            int i = 1;
            for (List<LasPointGeometry> byX : groupedByX) {
                Main.savePoints("/home/ricka/Lidar/groups/Cleaned/group" + i++ + ".geojson", byX);
            }

            i = 1;
            for (List<LasPointGeometry> byX : cleanedGroupedByX) {
                Main.savePoints("/home/ricka/Lidar/groups/NOT/group" + i++ + ".geojson", byX);
            }


            if(true){
                return List.of();
            }

            return inclinedLines.stream().flatMap(List::stream).map(l -> new InclinedSurface(new ArrayList<>(List.of(l)))).toList();
        }

        return mergeCloseInclinedLines(inclinedLines);
    }

    public List<InclinedSurface> mergeCloseInclinedLines(List<List<InclinedLine>> groupedInclinedLines) {
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

                        if (currentSurface.isLastLineMergeableWith(otherLine, 20, /* Const value */
                                conf.epsilonY(), 1, conf.epsilonSlope())) {
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

    private List<List<InclinedLine>> toGroupedInlinedLines(List<List<LasPointGeometry>> lasPointGroupedByX) {
        return lasPointGroupedByX.stream().map(points -> InclinedLine.from(points, 2, 0.1)).toList();
    }

    public record InclinedSurfaceSeparatorConf(int minimumPointCount, double epsilonX, double epsilonY, double epsilonZ, double epsilonSlope) {
        public InclinedSurfaceSeparatorConf() {
            this(DEFAULT_MINIMUM_POINT_COUNT, DEFAULT_EPSILON_X, DEFAULT_EPSILON_Y, DEFAULT_EPSILON_Z, DEFAULT_EPSILON_SLOPE);
        }
    }
}
