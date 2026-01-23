package app.bpartners.geojobs.model.lidar.planes;

import lombok.Builder;

@Builder(toBuilder = true)
public record Plane3DExtractorConf(
    BoxConf boxConf,
    PlaneConf planeConf,
    KernelConf kernelConf,
    PlaneMergerConf planeMergerConf,
    PlaneExtractionConf planeExtractionConf,
    RoofPointsCleanerConf roofPointsCleanerConf,
    PlaneDelimitationConf planeDelimitationConf) {

  @Builder(toBuilder = true)
  public record PlaneConf(
      double min2DArea,
      int minPointsCount,
      double minEdgeLength,
      double parallelDirectionEpsilon) {}

  @Builder(toBuilder = true)
  public record PlaneDelimitationConf(double concaveRatio, double simplificationEpsilon) {}

  @Builder(toBuilder = true)
  public record PlaneExtractionConf(int iteration, double pointContinuationThreshold) {}

  @Builder(toBuilder = true)
  public record PlaneMergerConf(double slopeEpsilon, double distanceEpsilon) {}

  @Builder(toBuilder = true)
  public record BoxConf(double threshold) {}

  @Builder(toBuilder = true)
  public record RoofPointsCleanerConf(double duplicateXYTolerance) {}

  @Builder(toBuilder = true)
  public record KernelConf(
      int attempts,
      int maxNeighborsCount,
      double threshold,
      double minVectorNorm,
      double orthogonalDegEpsilon) {}

  public static Plane3DExtractorConf getDefault() {
    return Plane3DExtractorConf.builder()
        .roofPointsCleanerConf(RoofPointsCleanerConf.builder().duplicateXYTolerance(0.3).build())
        .planeConf(
            PlaneConf.builder()
                .min2DArea(0.25)
                .minEdgeLength(0.75)
                .parallelDirectionEpsilon(15)
                .minPointsCount(10)
                .build())
        .boxConf(BoxConf.builder().threshold(0.25).build())
        .kernelConf(
            KernelConf.builder()
                .attempts(20)
                .threshold(0.75)
                .minVectorNorm(1e-6)
                .maxNeighborsCount(20)
                .orthogonalDegEpsilon(4)
                .build())
        .planeDelimitationConf(
            PlaneDelimitationConf.builder().concaveRatio(0.2).simplificationEpsilon(0.6).build())
        .planeMergerConf(PlaneMergerConf.builder().slopeEpsilon(10).distanceEpsilon(0.7).build())
        .planeExtractionConf(
            PlaneExtractionConf.builder().iteration(200).pointContinuationThreshold(0.5).build())
        .build();
  }
}
