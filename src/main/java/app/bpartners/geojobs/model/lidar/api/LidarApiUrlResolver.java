package app.bpartners.geojobs.model.lidar.api;

import static java.util.stream.Collectors.toSet;

import app.bpartners.geojobs.model.lidar.CrsProjector;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.springframework.web.client.RestClientException;

@AllArgsConstructor
@Slf4j
public class LidarApiUrlResolver {
  private static final Set<String> ALLOWED_URL_PREFIXES =
      Set.of(
          "https://storage.sbg.cloud.ovh.net/",
          "https://lidar.data.gouv.fr/",
          "https://data.geo.admin.ch/",
          "https://data.geopf.fr/");

  private final IgnLidarApi ignLidarApi;
  private final OpenSourceLidarApi openSourceLidarApi;
  private final SwissLidarApi swissLidarApi;

  public Set<String> resolveUrls(Geometry wgs84Geometry) {
    var envelope = wgs84Geometry.getEnvelopeInternal();

    if (CrsProjector.INSTANCE.isInSwiss(wgs84Geometry)) {
      return getSafeUrls(envelope, swissLidarApi);
    }

    Set<String> urls;
    try {
      urls = getSafeUrls(envelope, openSourceLidarApi);
    } catch (RestClientException e) {
      log.warn("OpenSourceLidarAPI call failed, using IGN as fallback", e);
      urls = Set.of();
    }

    return urls.isEmpty() ? getSafeUrls(envelope, ignLidarApi) : urls;
  }

  private static Set<String> getSafeUrls(Envelope envelope, LidarApi api) {
    return api.apply(envelope).stream().filter(LidarApiUrlResolver::isSafeUrl).collect(toSet());
  }

  private static boolean isSafeUrl(String url) {
    return url != null && ALLOWED_URL_PREFIXES.stream().anyMatch(url::startsWith);
  }
}
