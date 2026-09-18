package app.bpartners.geojobs.model.lidar.api;

import static java.util.stream.Collectors.toSet;

import app.bpartners.geojobs.file.bucket.WalloniaBucketComponent;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Envelope;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LidarUrlSafety {
  private static final Set<String> ALLOWED_URL_PREFIXES =
      Set.of(
          "https://storage.sbg.cloud.ovh.net/",
          "https://lidar.data.gouv.fr/",
          "https://data.geo.admin.ch/",
          "https://data.geopf.fr/");

  private final WalloniaBucketComponent walloniaBucketComponent;

  public Set<String> resolveSafe(Envelope envelope, LidarApi api) {
    return filterSafe(api.apply(envelope));
  }

  public Set<String> filterSafe(Set<String> urls) {
    return urls.stream().filter(this::isSafe).collect(toSet());
  }

  private boolean isSafe(String url) {
    if (url == null) return false;
    if (ALLOWED_URL_PREFIXES.stream().anyMatch(url::startsWith)) return true;
    return url.startsWith("https://" + walloniaBucketComponent.getBucketName() + ".s3.");
  }
}
