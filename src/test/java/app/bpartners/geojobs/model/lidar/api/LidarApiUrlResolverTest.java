package app.bpartners.geojobs.model.lidar.api;

import static app.bpartners.geojobs.conf.EnvConf.IGN_LIDAR_API_URL;
import static app.bpartners.geojobs.conf.EnvConf.OPEN_SOURCE_LIDAR_API_URL;
import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.OK;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

class LidarApiUrlResolverTest {
  RestTemplate restTemplateMock = mock();
  LidarApiUrlResolver subject =
      new LidarApiUrlResolver(
          new IgnLidarApi(new IgnLidarApiConf(IGN_LIDAR_API_URL), restTemplateMock),
          new OpenSourceLidarApi(
              new OpenSourceLidarApiConf(OPEN_SOURCE_LIDAR_API_URL), restTemplateMock),
          new SwissLidarApi(restTemplateMock));

  private static final String LAZ_FILE_URL = "https://data.geopf.fr/dummy.laz";

  @Test
  void resolve_urls_from_open_source_api_ok() {
    when(restTemplateMock.getForEntity(any(String.class), eq(FeatureCollection.class)))
        .thenReturn(openSourceApiResponse(LAZ_FILE_URL));

    var actual = subject.resolveUrls(frenchGeometry());

    assertEquals(Set.of(LAZ_FILE_URL), actual);
  }

  @Test
  void resolve_urls_from_ign_api_when_open_source_api_is_empty() {
    when(restTemplateMock.getForEntity(any(String.class), eq(FeatureCollection.class)))
        .thenReturn(emptyResponse())
        .thenReturn(ignApiResponse(LAZ_FILE_URL));

    var actual = subject.resolveUrls(frenchGeometry());

    assertEquals(Set.of(LAZ_FILE_URL), actual);
  }

  @Test
  void resolve_urls_from_ign_api_when_open_source_api_times_out() {
    when(restTemplateMock.getForEntity(any(String.class), eq(FeatureCollection.class)))
        .thenThrow(new ResourceAccessException("I/O error on GET request: Connection timed out"))
        .thenReturn(ignApiResponse(LAZ_FILE_URL));

    var actual = subject.resolveUrls(frenchGeometry());

    assertEquals(Set.of(LAZ_FILE_URL), actual);
  }

  @Test
  void resolve_urls_from_swiss_api_when_geometry_is_in_swiss_area() {
    when(restTemplateMock.getForObject(any(URI.class), eq(JsonNode.class)))
        .thenReturn(swissApiResponse());

    var actual = subject.resolveUrls(swissGeometry());

    assertTrue(
        actual.contains(
            "https://data.geo.admin.ch/ch.swisstopo.swisssurface3d/swisssurface3d_2025_2505-1119/swisssurface3d_2025_2505-1119_2056_5728.copc.laz"));
  }

  private static JsonNode swissApiResponse() {
    try {
      return new ObjectMapper()
          .readTree(
              """
{
  "type": "FeatureCollection",
  "features": [
    {
      "assets": {
        "swisssurface3d_2025_2505-1119_2056_5728.copc.laz": {
          "href": "https://data.geo.admin.ch/ch.swisstopo.swisssurface3d/swisssurface3d_2025_2505-1119/swisssurface3d_2025_2505-1119_2056_5728.copc.laz"
        }
      }
    }
  ]
}
""");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static ResponseEntity<FeatureCollection> emptyResponse() {
    return new ResponseEntity<>(FeatureCollection.builder().features(List.of()).build(), OK);
  }

  private static ResponseEntity<FeatureCollection> ignApiResponse(String url) {
    return new ResponseEntity<>(
        FeatureCollection.builder()
            .features(
                List.of(FeatureCollection.Feature.builder().properties(Map.of("url", url)).build()))
            .build(),
        OK);
  }

  private static ResponseEntity<FeatureCollection> openSourceApiResponse(String url) {
    return new ResponseEntity<>(
        FeatureCollection.builder()
            .features(
                List.of(
                    FeatureCollection.Feature.builder()
                        .assets(new FeatureCollection.Feature.Assets(Map.of("href", url)))
                        .build()))
            .build(),
        OK);
  }

  private static Geometry frenchGeometry() {
    var coordinates =
        new Coordinate[] {
          new Coordinate(2.243891733457616, 48.82448842864014),
          new Coordinate(2.243947393505863, 48.82437718542337),
          new Coordinate(2.244038835011281, 48.82440597780899),
          new Coordinate(2.2440209442821413, 48.82445309258651),
          new Coordinate(2.243891733457616, 48.82448842864014)
        };
    return geometryFactory.createPolygon(coordinates);
  }

  private static Geometry swissGeometry() {
    var coordinates =
        new Coordinate[] {
          new Coordinate(6.220857751344101, 46.218330151666642),
          new Coordinate(6.220048781018293, 46.218321418896565),
          new Coordinate(6.219912346396907, 46.217758267190007),
          new Coordinate(6.221001698608311, 46.217607435687704),
          new Coordinate(6.220857751344101, 46.218330151666642)
        };
    return geometryFactory.createPolygon(coordinates);
  }
}
