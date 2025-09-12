package app.bpartners.geojobs.service.lidar.preprocessing;

import app.bpartners.geojobs.service.lidar.model.LasPointGeometry;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record SolPointsCleaner(
    DuplicateXYPointsCleaner duplicateXYPointsCleaner,
    PointsZContinuationClusterExtractor pointsZContinuationClusterExtractor) {
  private static final double XY_TOLERANCE_METERS = 0.3;
  private static final double Z_DISCONTINUITY_THRESHOLD = 0.5;

  public SolPointsCleaner() {
    this(
        new DuplicateXYPointsCleaner(
            XY_TOLERANCE_METERS, DuplicateXYPointsCleaner.DuplicateXYPointToKeep.LOWEST),
        new PointsZContinuationClusterExtractor(Z_DISCONTINUITY_THRESHOLD));
  }

  public Set<LasPointGeometry> compute(Set<LasPointGeometry> solPoints) {
    var withoutDuplicateOnXY = duplicateXYPointsCleaner.compute(solPoints);
    var clusters = pointsZContinuationClusterExtractor.compute(withoutDuplicateOnXY);

    var mainCluster = clusters.stream().max(Comparator.comparingInt(List::size)).orElse(List.of());

    return new HashSet<>(mainCluster);
  }
}
