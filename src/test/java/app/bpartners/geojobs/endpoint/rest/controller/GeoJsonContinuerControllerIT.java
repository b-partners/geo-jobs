package app.bpartners.geojobs.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.service.geojson.GeoJsonContinuerService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;

@Slf4j
public class GeoJsonContinuerControllerIT extends FacadeIT {
  @MockBean GeoJsonContinuerController subject;
  private static final Integer imgSize = 1_024;
  private static final Integer zoom = 17;
  @Autowired GeoJsonContinuerService geoJsonContinuerService;
  @MockBean BucketComponent bucketComponentMock;

  @Test
  void test_continue_geojson_controller() throws Exception {

    MockMultipartFile file =
        new MockMultipartFile("file", "input.geojson", "application/json", "{}".getBytes());

    when(subject.continueGeoJson(file, imgSize, zoom)).thenReturn("https://url-presigned");

    String resultUrl = subject.continueGeoJson(file, imgSize, zoom);

    verify(subject).continueGeoJson(file, imgSize, zoom);

    assertEquals("https://url-presigned", resultUrl);
  }
}
