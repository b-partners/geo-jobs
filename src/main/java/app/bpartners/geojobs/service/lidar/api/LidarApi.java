package app.bpartners.geojobs.service.lidar.api;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;

import java.io.File;
import java.io.FileOutputStream;
import java.util.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@AllArgsConstructor
@Slf4j
public class LidarApi {
  private final LidarApiConf conf;
  private final RestTemplate restTemplate;
  private final LidarApiFallbackUrl fallbackUrl;
  private static final String UPDATED_DATA_PREFIX = "https://data.geopf.fr/";
  private static final Set<String> ALLOWED_URL_PREFIXES =
      Set.of(
          "https://storage.sbg.cloud.ovh.net/", "https://lidar.data.gouv.fr/", UPDATED_DATA_PREFIX);

  private static final long UPDATED_VALID_DATA = 50_000_000;

  public Map<String, Set<Geometry>> getUniqueLidarFilesUrls(Collection<Geometry> geometries) {
    Map<String, Set<Geometry>> filesUrls = new HashMap<>();

    for (var geometry : geometries) {
      var envelope = geometry.getEnvelopeInternal();
      var features = getFeatures(envelope);
      var urls = getUrlsFromFeatures(features);

      if (urls.isEmpty()) {
        urls = fallbackUrl.getUniqueLidarUrlsForUnavailableData(envelope);
      } else {
        urls = handleDeprecatedData(envelope, urls);
      }

      for (var url : urls) {
        log.info("LAZ File to download: {}", url);
        filesUrls.computeIfAbsent(url, key -> new HashSet<>()).add(geometry);
      }
    }

    return filesUrls;
  }

  public Optional<File> download(String fileUrl) {
    if (!isSafeUrl(fileUrl)) {
      log.warn("Unsafe URL blocked: {}", fileUrl);
      return Optional.empty();
    }

    log.info("Downloading {}", fileUrl);
    try {
      byte[] data = restTemplate.getForObject(fileUrl, byte[].class);
      if (data == null) {
        return Optional.empty();
      }

      var tempFile = File.createTempFile("lidar-", ".laz");
      try (var outputStream = new FileOutputStream(tempFile)) {
        outputStream.write(data);
      }

      log.info("Finished downloading {}", fileUrl);
      return Optional.of(tempFile);
    } catch (HttpClientErrorException.NotFound e) {
      log.warn("File not found (404): {}", fileUrl);
      return Optional.empty();
    } catch (Exception e) {
      log.error("Failed to download LAZ file from {}", fileUrl, e);
      throw new RuntimeException("Could not download file: " + fileUrl, e);
    }
  }

  private Set<String> handleDeprecatedData(Envelope envelope, Set<String> urls) {
    if (urls.stream().allMatch(url -> url.startsWith(UPDATED_DATA_PREFIX))) {
      return urls;
    }

    var hasValidContent = urls.stream().allMatch(this::hasValidContent);
    if (hasValidContent) {
      return urls;
    }

    return fallbackUrl.getUniqueLidarUrlsForDeprecatedData(envelope);
  }

  private boolean hasValidContent(String url) {
    try {
      var headers = restTemplate.headForHeaders(url);
      long contentLength = headers.getContentLength();
      log.info("Content-Length={} for fileUrl={}", contentLength, url);
      return contentLength > UPDATED_VALID_DATA;
    } catch (Exception e) {
      log.warn("Cannot get the Content-Length of the fileUrl={}", url);
      return true;
    }
  }

  @SuppressWarnings("all")
  private List<FeatureCollection.Feature> getFeatures(Envelope envelope) {
    var uriBuilder = UriComponentsBuilder.fromHttpUrl(conf.getUrl());
    conf.getDefaultParams(envelope).forEach(uriBuilder::queryParam);
    return requireNonNull(
            restTemplate.getForEntity(uriBuilder.toUriString(), FeatureCollection.class).getBody())
        .getFeatures();
  }

  private static Set<String> getUrlsFromFeatures(List<FeatureCollection.Feature> features) {
    return features.stream()
        .map(feature -> feature.getProperties().get("url").toString())
        .collect(toSet());
  }

  private static boolean isSafeUrl(String url) {
    if (url == null) return false;
    return ALLOWED_URL_PREFIXES.stream().anyMatch(url::startsWith);
  }
}
