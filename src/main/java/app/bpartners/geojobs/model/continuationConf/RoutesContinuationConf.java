package app.bpartners.geojobs.model.continuationConf;

import static java.lang.Math.PI;

import lombok.Getter;

@Getter
public enum RoutesContinuationConf {
  DEFAULT_MIN_COVERAGE_ABS_AREA(0.5),
  DEFAULT_MIN_ABS_AREA(1),
  DEFAULT_BUFFER(1),
  DEFAULT_MIN_DIRECTION_THRESHOLD(PI / 12),
  DEFAULT_MAX_DIRECTION_THRESHOLD(PI / 6),
  DEFAULT_DISTANCE_THRESHOLD(500),
  DEFAULT_PRETTY_CONF(0);

  private final double value;

  RoutesContinuationConf(double value) {
    this.value = value;
  }
}
