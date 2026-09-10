package app.bpartners.geojobs.model.lidar.zone;

import static app.bpartners.geojobs.service.GeometrySquareMeterArea.LAMBERT_93;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.locationtech.jts.geom.Geometry;

public final class DefaultZone implements LidarZone {
  public static final DefaultZone INSTANCE = new DefaultZone();

  private DefaultZone() {}

  @Override
  public boolean contains(Geometry wgs84Geometry) {
    return true;
  }

  @Override
  public CoordinateReferenceSystem getLocalCrs() {
    return LAMBERT_93;
  }
}
