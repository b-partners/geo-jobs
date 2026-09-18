package app.bpartners.geojobs.model.lidar.api;

import app.bpartners.geojobs.model.lidar.CrsProjector;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Geometry;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LidarApiUrlResolver {
  private final CrsProjector crsProjector;

  public Set<String> resolveUrls(Geometry wgs84Geometry) {
    var zone = crsProjector.resolveZone(wgs84Geometry);
    return zone.resolveUrls(wgs84Geometry.getEnvelopeInternal());
  }
}
