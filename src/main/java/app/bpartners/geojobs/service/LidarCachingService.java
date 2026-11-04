package app.bpartners.geojobs.service;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

import app.bpartners.geojobs.endpoint.rest.model.CreateLidarCaching;
import app.bpartners.geojobs.endpoint.rest.model.LidarCaching;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.service.threed.ThreedApiClient;
import app.bpartners.geojobs.service.threed.exception.ThreedApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Has threed cache the lidar files around a point, so that the 3D generated there afterwards reads
 * them from cacher instead of the lidar source.
 */
@Service
@RequiredArgsConstructor
public class LidarCachingService {
  private final ThreedApiClient threedApiClient;

  /** threed refuses a point it can not read (400): it is the client's point, a bad request. */
  public LidarCaching cache(String id, CreateLidarCaching createLidarCaching) {
    try {
      return threedApiClient.cacheLidar(id, createLidarCaching);
    } catch (ThreedApiException e) {
      if (e.getStatusCode() != null && e.getStatusCode().isSameCodeAs(BAD_REQUEST)) {
        throw new BadRequestException(e.getApiError() != null ? e.getApiError() : e.getMessage());
      }
      throw e;
    }
  }
}
