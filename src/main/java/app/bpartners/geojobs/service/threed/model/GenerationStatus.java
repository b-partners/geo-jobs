package app.bpartners.geojobs.service.threed.model;

public enum GenerationStatus {
  SUCCEEDED,
  /** Generated, but part of the result is degraded, see {@link Lrg#getMessage()}. */
  PARTIAL,
  FAILED
}
