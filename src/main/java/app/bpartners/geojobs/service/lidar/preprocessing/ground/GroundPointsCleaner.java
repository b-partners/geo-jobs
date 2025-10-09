package app.bpartners.geojobs.service.lidar.preprocessing.ground;

import static app.bpartners.geojobs.service.lidar.model.geometry.Axis.Z;

import app.bpartners.geojobs.service.lidar.model.geometry.LasPointGeometry;
import app.bpartners.geojobs.service.lidar.preprocessing.ContinuationClusterExtractor;
import app.bpartners.geojobs.service.lidar.preprocessing.DuplicatePointsOnTwoAxesCleaner;
import java.util.*;
import java.util.function.Function;

public record GroundPointsCleaner(
    DuplicatePointsOnTwoAxesCleaner duplicateXYPointsCleaner,
    ContinuationClusterExtractor pointsZContinuationClusterExtractor)
    implements Function<Collection<LasPointGeometry>, Set<LasPointGeometry>> {
  private static final double XY_TOLERANCE_METERS = 0.3;
  private static final double Z_DISCONTINUITY_THRESHOLD = 0.5;

  public GroundPointsCleaner() {
    this(
        DuplicatePointsOnTwoAxesCleaner.xyKeepHighest(XY_TOLERANCE_METERS, XY_TOLERANCE_METERS),
        new ContinuationClusterExtractor(Z, Z_DISCONTINUITY_THRESHOLD));
  }

  @Override
  public Set<LasPointGeometry> apply(Collection<LasPointGeometry> solPoints) {
    var withoutDuplicateOnXY = duplicateXYPointsCleaner.apply(solPoints);
    var clusters = pointsZContinuationClusterExtractor.apply(withoutDuplicateOnXY);
    var mainCluster = clusters.stream().max(Comparator.comparingInt(List::size)).orElse(List.of());

    return new HashSet<>(mainCluster);
  }
}
