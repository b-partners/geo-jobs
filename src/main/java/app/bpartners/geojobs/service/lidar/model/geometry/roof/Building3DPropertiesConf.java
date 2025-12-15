package app.bpartners.geojobs.service.lidar.model.geometry.roof;

import lombok.Builder;

@Builder(toBuilder = true)
public record Building3DPropertiesConf(
    PlaneConf planeConf,
    PlaneMergerConf planeMergerConf,
    PlaneExtractionConf planeExtractionConf,
    PlaneDelimitationConf planeDelimitationConf) {

  @Builder(toBuilder = true)
  public record PlaneConf(int minPointsCount, double min2DArea) {}

  @Builder(toBuilder = true)
  public record PlaneDelimitationConf(double concaveRatio, double simplificationEpsilon) {}

  @Builder(toBuilder = true)
  public record PlaneExtractionConf(
      int iteration, double pointThreshold, double pointContinuationThreshold) {}

  @Builder(toBuilder = true)
  public record PlaneMergerConf(double slopeEpsilon, double distanceEpsilon, double max2DArea) {}

  public static Building3DPropertiesConf getDefault() {
    return Building3DPropertiesConf.builder()
        .planeConf(PlaneConf.builder().min2DArea(0.25).minPointsCount(10).build())
        .planeDelimitationConf(
            PlaneDelimitationConf.builder().concaveRatio(0.2).simplificationEpsilon(0.6).build())
        .planeMergerConf(
            PlaneMergerConf.builder().max2DArea(0.2).slopeEpsilon(10).distanceEpsilon(0.5).build())
        .planeExtractionConf(
            PlaneExtractionConf.builder()
                .iteration(200)
                .pointThreshold(0.2)
                .pointContinuationThreshold(1)
                .build())
        .build();
  }
}
