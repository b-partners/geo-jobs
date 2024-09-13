package app.bpartners.geojobs.repository.model;

import app.bpartners.geojobs.job.model.JobType;

public enum GeoJobType implements JobType {
  TILING,
  DETECTION,
  PARCEL_DETECTION,
  ANNOTATION_DELIVERY
}
