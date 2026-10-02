package app.bpartners.geojobs.service.lidar.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Geometry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

/** Calls bpartners-geodata's {@code POST /lidar/urls} to resolve LIDAR file URLs for geometries. */
@Component
@Slf4j
public class GeodataLidarApiClient {
  private static final String API_KEY_HEADER = "x-api-key";

  private final ObjectMapper om;
  private final RestTemplate restTemplate;
  private final String geodataApiUrl;
  private final String geodataApiKey;

  public GeodataLidarApiClient(
      ObjectMapper om,
      RestTemplate restTemplate,
      @Value("${geodata.api.url}") String geodataApiUrl,
      @Value("${geodata.api.key}") String geodataApiKey) {
    this.om = om;
    this.restTemplate = restTemplate;
    this.geodataApiUrl = geodataApiUrl;
    this.geodataApiKey = geodataApiKey;
  }

  public record LidarUrlsResult(String region, String crs, List<Set<String>> urlsPerGeometry) {}

  @SneakyThrows
  public LidarUrlsResult getLidarFileUrls(List<Geometry> wgs84Geometries) {
    var headers = new HttpHeaders();
    headers.add("Content-Type", "application/json");
    headers.add(API_KEY_HEADER, geodataApiKey);

    var geometriesJson = wgs84Geometries.stream().map(GeoJsonGeometryWriter::toGeoJson).toList();
    var requestBody = om.writeValueAsString(Map.of("geometries", geometriesJson));
    var request = new HttpEntity<>(requestBody, headers);

    try {
      var response =
          restTemplate.postForEntity(
              geodataApiUrl + "/lidar/urls", request, LidarUrlsResponse.class);
      var body = response.getBody();
      if (response.getStatusCode().value() != 200 || body == null) {
        throw new IllegalStateException(
            "Error while calling geodata API for lidar urls at "
                + geodataApiUrl
                + " with HTTP code "
                + response.getStatusCode());
      }
      var urlsPerGeometry =
          body.results() == null
              ? List.<Set<String>>of()
              : body.results().stream()
                  .map(item -> item.urls() == null ? Set.<String>of() : Set.copyOf(item.urls()))
                  .toList();
      return new LidarUrlsResult(body.region(), body.crs(), urlsPerGeometry);
    } catch (HttpStatusCodeException e) {
      throw new IllegalStateException(
          "Error while calling geodata API for lidar urls at "
              + geodataApiUrl
              + " with exception "
              + e.getMessage(),
          e);
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record LidarUrlsResponse(String region, String crs, List<LidarUrlsResultItem> results) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record LidarUrlsResultItem(List<String> urls) {}
}
