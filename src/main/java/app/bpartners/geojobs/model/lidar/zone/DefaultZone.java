package app.bpartners.geojobs.model.lidar.zone;

import static app.bpartners.geojobs.service.GeometrySquareMeterArea.LAMBERT_93;

import app.bpartners.geojobs.model.lidar.api.IgnLidarApi;
import app.bpartners.geojobs.model.lidar.api.LidarUrlSafety;
import app.bpartners.geojobs.model.lidar.api.OpenSourceLidarApi;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@RequiredArgsConstructor
@Slf4j
public class DefaultZone implements LidarZone {
  private final OpenSourceLidarApi openSourceLidarApi;
  private final IgnLidarApi ignLidarApi;
  private final LidarUrlSafety urlSafety;

  @Override
  public boolean contains(Geometry wgs84Geometry) {
    return true;
  }

  @Override
  public CoordinateReferenceSystem getLocalCrs() {
    return LAMBERT_93;
  }

  @Override
  public Set<String> resolveUrls(Envelope envelope) {
    Set<String> urls;
    try {
      urls = urlSafety.resolveSafe(envelope, openSourceLidarApi);
    } catch (RestClientException e) {
      log.warn("OpenSourceLidarAPI call failed, using IGN as fallback", e);
      urls = Set.of();
    }

    return urls.isEmpty() ? urlSafety.resolveSafe(envelope, ignLidarApi) : urls;
  }
}
