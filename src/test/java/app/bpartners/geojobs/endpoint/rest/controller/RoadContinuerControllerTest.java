package app.bpartners.geojobs.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TilingConf;
import app.bpartners.geojobs.service.BucketServiceRoadContinuer;
import app.bpartners.geojobs.service.RoadContinuerService;
import java.io.File;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RoadContinuerControllerTest {

  @Mock private RoadContinuerService roadContinuerService;

  @Mock private BucketServiceRoadContinuer bsrc;

  @InjectMocks private RoadContinuerController controller;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(controller, "adminApiKey", "dummyApiKey");
  }

  @Test
  void testRoadContinuer_shouldReturnPresignedUrl() throws Exception {
    String geojsonInput = "{\"type\":\"FeatureCollection\",\"features\":[]}";
    Integer zoom = 12;
    Integer imageSize = 1_024;
    File mockGeoJsonFile = new File("mock.geojson");
    var tilingConf = new TilingConf(zoom, imageSize);
    var expected = Map.of("url", "https://presigned.url/mock.geojson");

    when(roadContinuerService.getTilingConf(zoom, imageSize)).thenReturn(tilingConf);
    when(roadContinuerService.continueRoute(geojsonInput, tilingConf)).thenReturn(mockGeoJsonFile);
    when(bsrc.getContinuedRoutePresignedUrl(mockGeoJsonFile, "dummyApiKey")).thenReturn(expected);

    var actual = controller.roadContinuer(geojsonInput, zoom, imageSize);

    assertEquals(expected, actual);
    verify(roadContinuerService).getTilingConf(zoom, imageSize);
    verify(roadContinuerService).continueRoute(geojsonInput, tilingConf);
    verify(bsrc).getContinuedRoutePresignedUrl(mockGeoJsonFile, "dummyApiKey");
  }
}
