package app.bpartners.geojobs.model.lidar.zone;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.file.bucket.WalloniaBucketComponent;
import app.bpartners.geojobs.model.lidar.api.LidarUrlSafety;
import app.bpartners.geojobs.model.lidar.api.SwissLidarApi;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.springframework.web.client.RestTemplate;

class SwissZoneTest {
  RestTemplate restTemplateMock = mock();
  WalloniaBucketComponent walloniaBucketComponentMock = mock();
  SwissZone subject =
      new SwissZone(
          new SwissLidarApi(restTemplateMock), new LidarUrlSafety(walloniaBucketComponentMock));

  @Test
  void resolve_urls_from_swiss_api_when_geometry_is_in_swiss_area() {
    when(restTemplateMock.getForObject(any(URI.class), eq(JsonNode.class)))
        .thenReturn(swissApiResponse());

    var actual = subject.resolveUrls(swissGeometry().getEnvelopeInternal());

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
