package app.bpartners.geojobs.model.lidar;

import static app.bpartners.geojobs.service.GeometrySquareMeterArea.WGS84;

import app.bpartners.geojobs.model.lidar.zone.DefaultZone;
import app.bpartners.geojobs.model.lidar.zone.LidarZone;
import app.bpartners.geojobs.model.lidar.zone.SwissZone;
import app.bpartners.geojobs.service.GeometrySquareMeterArea;
import java.util.List;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.locationtech.jts.geom.Geometry;

public final class CrsProjector {
  public static final CrsProjector INSTANCE = new CrsProjector();

  private static final List<LidarZone> ZONES = List.of(SwissZone.INSTANCE, DefaultZone.INSTANCE);
  private static final GeometrySquareMeterArea projector = new GeometrySquareMeterArea();

  private CrsProjector() {}

  public LidarZone resolveZone(Geometry wgs84Geometry) {
    return ZONES.stream().filter(zone -> zone.contains(wgs84Geometry)).findFirst().orElseThrow();
  }

  public boolean isInSwiss(Geometry wgs84Geometry) {
    return resolveZone(wgs84Geometry) == SwissZone.INSTANCE;
  }

  public CoordinateReferenceSystem getLocalCrs(Geometry wgs84Geometry) {
    return resolveZone(wgs84Geometry).getLocalCrs();
  }

  public Geometry projectToLocalCrs(Geometry wgs84Geometry) {
    return projector.project(wgs84Geometry, WGS84, getLocalCrs(wgs84Geometry));
  }
}
