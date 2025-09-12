package app.bpartners.geojobs.service.lidar.preprocessing;

import app.bpartners.geojobs.service.lidar.model.LasPointGeometry;
import java.util.*;

public record RoofPointsCleaner(
    DuplicateXYPointsCleaner duplicateXYPointsCleaner,
    PointsZContinuationClusterExtractor pointsZContinuationClusterExtractor) {
  private static final double XY_TOLERANCE_METERS = 0.3;
  private static final double Z_DISCONTINUITY_THRESHOLD = 0.5;

  public RoofPointsCleaner() {
    this(
        new DuplicateXYPointsCleaner(
            XY_TOLERANCE_METERS, DuplicateXYPointsCleaner.DuplicateXYPointToKeep.HIGHEST),
        new PointsZContinuationClusterExtractor(Z_DISCONTINUITY_THRESHOLD));
  }

  public Set<LasPointGeometry> compute(Set<LasPointGeometry> roofPoints) {
    var withoutDuplicateOnXY = duplicateXYPointsCleaner.compute(roofPoints);
    var clusters = pointsZContinuationClusterExtractor.compute(withoutDuplicateOnXY);

    var mainCluster = clusters.stream().max(Comparator.comparingInt(List::size)).orElse(List.of());

    return new HashSet<>(mainCluster);
  }
}
