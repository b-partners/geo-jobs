package app.bpartners.geojobs.model.lidar.api;

import static java.util.stream.Collectors.toSet;

import app.bpartners.geojobs.model.lidar.CrsProjector;
import app.bpartners.geojobs.model.lidar.zone.DefaultZone;
import app.bpartners.geojobs.model.lidar.zone.LidarZone;
import app.bpartners.geojobs.model.lidar.zone.SwissZone;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
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
    var zone = CrsProjector.INSTANCE.resolveZone(wgs84Geometry);
    var envelope = wgs84Geometry.getEnvelopeInternal();
    return urlResolversByZone().getOrDefault(zone, this::resolveDefaultZoneUrls).apply(envelope);
  }

  private Map<LidarZone, Function<Envelope, Set<String>>> urlResolversByZone() {
    return Map.of(
        SwissZone.INSTANCE, envelope -> getSafeUrls(envelope, swissLidarApi),
        DefaultZone.INSTANCE, this::resolveDefaultZoneUrls);
  }

  private Set<String> resolveDefaultZoneUrls(Envelope envelope) {
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
