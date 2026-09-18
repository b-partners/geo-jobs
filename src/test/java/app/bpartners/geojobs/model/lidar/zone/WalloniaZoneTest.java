package app.bpartners.geojobs.model.lidar.zone;

import static app.bpartners.geojobs.conf.EnvConf.WALLONIA_LIDAR_API_URL;
import static app.bpartners.geojobs.service.model.WalloniaBoundaryCheckerTest.liege_coords;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.file.bucket.WalloniaBucketComponent;
import app.bpartners.geojobs.model.lidar.api.LidarUrlSafety;
import app.bpartners.geojobs.service.lidar.api.WalloniaLidarApi;
import app.bpartners.geojobs.service.lidar.api.WalloniaLidarApiConf;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

class WalloniaZoneTest {
  RestTemplate restTemplateMock = mock();
  WalloniaBucketComponent walloniaBucketComponentMock = mock();
  WalloniaZone subject =
      new WalloniaZone(
          new WalloniaLidarApi(
              new WalloniaLidarApiConf(WALLONIA_LIDAR_API_URL),
              restTemplateMock,
              walloniaBucketComponentMock),
          new LidarUrlSafety(walloniaBucketComponentMock));

  @Test
  void resolve_urls_from_wallonia_bucket_when_geometry_is_in_wallonia_area() {
    when(restTemplateMock.getForObject(any(URI.class), eq(JsonNode.class)))
        .thenReturn(walloniaApiResponse());
    when(walloniaBucketComponentMock.getBucketName()).thenReturn("dummy-wallonia-bucket");
    when(walloniaBucketComponentMock.presign(any()))
        .thenAnswer(
            invocation ->
                "https://dummy-wallonia-bucket.s3.eu-west-3.amazonaws.com/"
                    + invocation.getArgument(0));

    var actual = subject.resolveUrls(liege_coords().getEnvelopeInternal());

    assertTrue(
        actual.stream().allMatch(url -> url.startsWith("https://dummy-wallonia-bucket.s3.")));
  }

  private static JsonNode walloniaApiResponse() {
    try {
      return new ObjectMapper()
          .readTree(
              """
{
  "features": [
    {
      "attributes": {
        "LAS_NAME": "dummy_tile"
      }
    }
  ]
}
""");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
