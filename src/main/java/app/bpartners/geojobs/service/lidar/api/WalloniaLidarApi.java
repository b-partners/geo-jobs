package app.bpartners.geojobs.service.lidar.api;

import static java.util.stream.Collectors.toSet;

import app.bpartners.geojobs.file.bucket.BucketComponent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.Objects;
import java.util.Set;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Envelope;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Queries the SPW (Service public de Wallonie) LIDAR_MAILLES tile index to find which LAZ tiles
 * cover a given envelope, then resolves each tile to a presigned S3 URL. The point clouds
 * themselves are downloaded and uploaded to S3 out of band (SPW only exposes a manual,
 * email-delayed download basket, no direct-download API for the LAZ files).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WalloniaLidarApi implements LidarApi {
  private static final String BUCKET_KEY_PREFIX = "lidar/wallonia/liege/";
  private static final String LAZ_FILE_SUFFIX = ".laz";

  private final WalloniaLidarApiConf conf;
  private final RestTemplate restTemplate;
  private final BucketComponent bucketComponent;

  @Override
  @SuppressWarnings("all")
  public Set<String> apply(Envelope envelope) {
    var uriBuilder = UriComponentsBuilder.fromHttpUrl(conf.getUrl());
    conf.getDefaultParams(envelope).forEach(uriBuilder::queryParam);
    var uri = uriBuilder.build().encode().toUri();

    JsonNode data = restTemplate.getForObject(uri, JsonNode.class);
    if (data == null || !(data.get("features") instanceof ArrayNode features)) {
      log.warn("No tiles found from Wallonia LIDAR_MAILLES service for envelope={}", envelope);
      return Set.of();
    }

    return StreamSupport.stream(features.spliterator(), false)
        .map(feature -> feature.path("attributes").path("LAS_NAME").asText(null))
        .filter(Objects::nonNull)
        .map(lasName -> bucketComponent.presign(BUCKET_KEY_PREFIX + lasName + LAZ_FILE_SUFFIX))
        .collect(toSet());
  }
}
