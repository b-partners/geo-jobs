package app.bpartners.geojobs.model.lidar;

import static app.bpartners.geojobs.service.GeometrySquareMeterArea.WGS84;

import app.bpartners.geojobs.model.lidar.zone.LidarZone;
import app.bpartners.geojobs.model.lidar.zone.SwissZone;
import app.bpartners.geojobs.model.lidar.zone.WalloniaZone;
import app.bpartners.geojobs.service.GeometrySquareMeterArea;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.locationtech.jts.geom.Geometry;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CrsProjector {
  private final List<LidarZone> zones;
  private final GeometrySquareMeterArea projector;

  public LidarZone resolveZone(Geometry wgs84Geometry) {
    return zones.stream().filter(zone -> zone.contains(wgs84Geometry)).findFirst().orElseThrow();
  }

  public boolean isInSwiss(Geometry wgs84Geometry) {
    return resolveZone(wgs84Geometry) instanceof SwissZone;
  }

  public boolean isInWallonia(Geometry wgs84Geometry) {
    return resolveZone(wgs84Geometry) instanceof WalloniaZone;
  }

  public CoordinateReferenceSystem getLocalCrs(Geometry wgs84Geometry) {
    return resolveZone(wgs84Geometry).getLocalCrs();
  }

  public Geometry projectToLocalCrs(Geometry wgs84Geometry) {
    return projector.project(wgs84Geometry, WGS84, getLocalCrs(wgs84Geometry));
  }
}
