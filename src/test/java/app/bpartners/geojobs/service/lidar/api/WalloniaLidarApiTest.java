package app.bpartners.geojobs.service.lidar.api;

import static app.bpartners.geojobs.service.model.WalloniaBoundaryCheckerTest.liege_coords;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.file.bucket.WalloniaBucketComponent;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

class WalloniaLidarApiTest {
  private static final String WALLONIA_LIDAR_API_URL =
      "https://geoservices.wallonie.be/arcgis/rest/services/RELIEF/LIDAR_MAILLES/MapServer/0/query";
  RestTemplate restTemplate = new RestTemplate();
  WalloniaBucketComponent bucketComponentMock = mock();
  WalloniaLidarApi subject =
      new WalloniaLidarApi(
          new WalloniaLidarApiConf(WALLONIA_LIDAR_API_URL), restTemplate, bucketComponentMock);

  @Test
  void retrieve_wallonia_lidar_tile_presigned_urls() {
    when(bucketComponentMock.presign(any()))
        .thenAnswer(
            invocation ->
                "https://dummy-bucket.s3.eu-west-3.amazonaws.com/" + invocation.getArgument(0));

    var res = subject.apply(liege_coords().getEnvelopeInternal());

    assertTrue(
        res.stream()
            .anyMatch(
                url ->
                    url.startsWith(
                        "https://dummy-bucket.s3.eu-west-3.amazonaws.com/wallonia/liege/")));
    assertTrue(res.stream().allMatch(url -> url.endsWith(".laz")));
  }
}
