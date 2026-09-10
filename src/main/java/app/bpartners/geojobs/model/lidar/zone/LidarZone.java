package app.bpartners.geojobs.model.lidar.zone;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.locationtech.jts.geom.Geometry;

public interface LidarZone {
  boolean contains(Geometry wgs84Geometry);

  CoordinateReferenceSystem getLocalCrs();
}
