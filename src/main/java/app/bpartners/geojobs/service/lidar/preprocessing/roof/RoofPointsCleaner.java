package app.bpartners.geojobs.service.lidar.preprocessing.roof;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import app.bpartners.geojobs.service.lidar.preprocessing.DuplicateXYPointsCleaner;
import app.bpartners.geojobs.service.lidar.preprocessing.PointsZContinuationClusterExtractor;
import java.util.*;

public record RoofPointsCleaner(
    DuplicateXYPointsCleaner duplicateXYPointsCleaner,
    PointsZContinuationClusterExtractor pointsZContinuationClusterExtractor) {
  private static final double XY_TOLERANCE_METERS = 0.3;
  private static final double Z_DISCONTINUITY_THRESHOLD = 0.1;

  public RoofPointsCleaner() {
    this(
        new DuplicateXYPointsCleaner(
            XY_TOLERANCE_METERS, DuplicateXYPointsCleaner.DuplicateXYPointToKeep.HIGHEST),
        new PointsZContinuationClusterExtractor(Z_DISCONTINUITY_THRESHOLD));
  }

  public Set<LasPointGeometry> apply(Collection<LasPointGeometry> roofPoints) {
    return duplicateXYPointsCleaner.compute(roofPoints);
  }
}
