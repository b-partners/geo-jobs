package app.bpartners.geojobs.service.lidar.api;

import static app.bpartners.geojobs.file.FileWriter.createTempDirectory;
import static app.bpartners.geojobs.service.GeometrySquareMeterArea.EPSG_2056;
import static app.bpartners.geojobs.service.GeometrySquareMeterArea.EPSG_3812;
import static app.bpartners.geojobs.service.GeometrySquareMeterArea.LAMBERT_93;
import static app.bpartners.geojobs.service.GeometrySquareMeterArea.WGS84;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toSet;

import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.service.GeometrySquareMeterArea;
import app.bpartners.geojobs.service.cacher.CacherApiClient;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.Paths;
import java.util.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
@AllArgsConstructor
@Slf4j
public class LidarApiFacade {
  private final IgnLidarApi ignLidarApi;
  private final OpenSourceLidarApi openSourceLidarApi;
  private final FallbackLidarApi fallbackLidarApi;
  private final SwissBoundaryChecker swissBoundaryChecker;
  private final SwissLidarApi swissLidarApi;
  private final IgnBrowserScraperLidarApi ignBrowserScraperLidarApi;
  private final WalloniaBoundaryChecker walloniaBoundaryChecker;
  private final WalloniaLidarApi walloniaLidarApi;
  private final GeometrySquareMeterArea projector;
  private final RestTemplate restTemplate;
  private final CacherApiClient cacherApiClient;
  private final BucketComponent bucketComponent;

  private static final String LAZ_FILE_SUFFIX = ".laz";
  private static final long UPDATED_VALID_DATA = 50_000_000;
  private static final String UPDATED_DATA_PREFIX = "https://data.geopf.fr/";
  private static final Set<String> ALLOWED_URL_PREFIXES =
      Set.of(
          "https://storage.sbg.cloud.ovh.net/",
          "https://lidar.data.gouv.fr/",
          "https://data.geo.admin.ch/",
          UPDATED_DATA_PREFIX);

  public Map<String, Set<Geometry>> getUniqueLidarFilesUrls(Collection<Geometry> wgs84Geometries) {
    Set<ProjectedGeometry> geometries = getProjectedGeometries(wgs84Geometries);
    Map<String, Set<Geometry>> filesUrls = new HashMap<>();
    var firstWgs84Geometry = geometries.stream().findFirst().map(ProjectedGeometry::wgs84);
    boolean isGeometryInSwiss =
        firstWgs84Geometry.map(swissBoundaryChecker::isGeometryInSwiss).orElse(false);
    boolean isGeometryInWallonia =
        !isGeometryInSwiss
            && firstWgs84Geometry.map(walloniaBoundaryChecker::isGeometryInWallonia).orElse(false);

    for (var geometry : geometries) {
      Set<String> urls;
      Geometry targetGeometry;
      if (isGeometryInSwiss) {
        urls = getLidarFilesUrls(geometry.wgs84().getEnvelopeInternal(), swissLidarApi);
        targetGeometry = geometry.epsg2056();
      } else if (isGeometryInWallonia) {
        urls = getLidarFilesUrls(geometry.wgs84().getEnvelopeInternal(), walloniaLidarApi);
        targetGeometry = geometry.epsg3812();
      } else {
        urls = resolveOpenSourceUrls(geometry);
        targetGeometry = geometry.lambert93();
      }

      for (var url : urls) {
        log.info("LAZ File to download: {}", url);
        filesUrls.computeIfAbsent(url, key -> new HashSet<>()).add(targetGeometry);
      }
    }

    return filesUrls;
  }

  private Set<String> resolveOpenSourceUrls(ProjectedGeometry geometry) {
    Set<String> urls;
    try {
      urls = getLidarFilesUrls(geometry.wgs84().getEnvelopeInternal(), openSourceLidarApi);
    } catch (RestClientException e) {
      log.warn("OpenSourceLidarAPI call failed, using IGN as fallback", e);
      urls = Set.of();
    }

    if (urls.isEmpty()) {
      log.info(
          "No LidarFiles found from the OpenSourceLidarAPI (Browser Teledetection Stac API), using"
              + " IGN Browser Scraper as fallback");
      urls = getLidarFilesUrls(geometry.wgs84().getEnvelopeInternal(), ignBrowserScraperLidarApi);
      if (urls.isEmpty()) {
        log.info("No LidarFiles found from the IGN Browser Scraper, using IGN API as fallback");
        urls = getLidarFilesUrls(geometry.lambert93().getEnvelopeInternal(), ignLidarApi);
        urls = handleDeprecatedData(geometry.lambert93().getEnvelopeInternal(), urls);
      }
    }

    return urls;
  }

  public Optional<File> download(String fileUrl) {
    return download(fileUrl, createTempDirectory());
  }

  @SuppressWarnings("all")
  @SneakyThrows
  private URL getCachedUrl(String fileUrl) {
    return cacherApiClient.getWithCache(new URL(fileUrl));
  }

  @SneakyThrows
  public Optional<File> download(String fileUrl, File directory) {
    if (!isSafeUrl(fileUrl)) {
      log.warn("Unsafe URL blocked: {}", fileUrl);
      return Optional.empty();
    }

    log.info("Resolving cached URL for fileUrl={}", fileUrl);
    var cachedUrl = getCachedUrl(fileUrl);
    log.info("Finished resolving cached URL={} for fileUrl={}", cachedUrl, fileUrl);

    log.info("Downloading {}", cachedUrl);
    try {
      var data = restTemplate.getForObject(URI.create(cachedUrl.toString()), byte[].class);
      if (data == null) {
        return Optional.empty();
      }

      var filename = randomUUID().toString();
      var outputPath = Paths.get(directory.getPath(), filename + LAZ_FILE_SUFFIX);
      FileWriter.write(outputPath, data);

      log.info("Finished downloading {}", cachedUrl);
      var file = outputPath.toFile();
      file.deleteOnExit();
      return Optional.of(file);
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

    return fallbackLidarApi.getUniqueLidarUrlsForDeprecatedData(envelope);
  }

  private boolean hasValidContent(String url) {
    try {
      if (!isSafeUrl(url)) {
        log.warn("Unsafe URL blocked for HEAD: {}", url);
        return false;
      }

      var headers = restTemplate.headForHeaders(url);
      long contentLength = headers.getContentLength();
      log.info("Content-Length={} for fileUrl={}", contentLength, url);
      return contentLength > UPDATED_VALID_DATA;
    } catch (Exception e) {
      log.warn("Cannot get the Content-Length of the fileUrl={}", url);
      return true;
    }
  }

  private Set<ProjectedGeometry> getProjectedGeometries(Collection<Geometry> wgs84Geometries) {
    return wgs84Geometries.stream()
        .map(
            wgs84Geometry -> {
              var lambert93 = projector.project(wgs84Geometry, WGS84, LAMBERT_93);
              var epsg2056 = projector.project(wgs84Geometry, WGS84, EPSG_2056);
              var epsg3812 = projector.project(wgs84Geometry, WGS84, EPSG_3812);
              return new ProjectedGeometry(lambert93, wgs84Geometry, epsg2056, epsg3812);
            })
        .collect(toSet());
  }

  private Set<String> getLidarFilesUrls(Envelope envelope, LidarApi api) {
    return api.apply(envelope).stream().filter(this::isSafeUrl).collect(toSet());
  }

  private boolean isSafeUrl(String url) {
    if (url == null) return false;
    if (ALLOWED_URL_PREFIXES.stream().anyMatch(url::startsWith)) return true;
    return url.startsWith("https://" + bucketComponent.getBucketName() + ".s3.");
  }

  @Builder
  private record ProjectedGeometry(
      Geometry lambert93, Geometry wgs84, Geometry epsg2056, Geometry epsg3812) {}
}
