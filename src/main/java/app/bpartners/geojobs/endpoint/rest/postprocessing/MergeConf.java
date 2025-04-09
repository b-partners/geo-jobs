package app.bpartners.geojobs.endpoint.rest.postprocessing;

public record MergeConf(int directionTolerance, int minXDistance, int minYDistance) {
}
