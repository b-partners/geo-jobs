package app.bpartners.geojobs.model.lidar.planes;

import lombok.Builder;

@Builder(toBuilder = true)
public record Plane3DExtractorConf(
    PlaneConf planeConf,
    PlaneMergerConf planeMergerConf,
    PlaneExtractionConf planeExtractionConf,
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
  public record PlaneExtractionConf(
      int iteration, double pointThreshold, double pointContinuationThreshold) {}

  @Builder(toBuilder = true)
  public record PlaneMergerConf(double slopeEpsilon, double distanceEpsilon) {}

  public static Plane3DExtractorConf getDefault() {
    return Plane3DExtractorConf.builder()
        .planeConf(
            PlaneConf.builder()
                .min2DArea(0.25)
                .minEdgeLength(0.75)
                .parallelDirectionEpsilon(15)
                .minPointsCount(10)
                .build())
        .planeDelimitationConf(
            PlaneDelimitationConf.builder().concaveRatio(0.2).simplificationEpsilon(0.6).build())
        .planeMergerConf(PlaneMergerConf.builder().slopeEpsilon(10).distanceEpsilon(0.7).build())
        .planeExtractionConf(
            PlaneExtractionConf.builder()
                .iteration(200)
                .pointThreshold(0.25)
                .pointContinuationThreshold(1)
                .build())
        .build();
  }
}
