package app.bpartners.geojobs.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TilingConf;
import app.bpartners.geojobs.service.RoadContinuerService;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoadContinuerControllerTest {

  @Mock private RoadContinuerService roadContinuerService;

  @InjectMocks private RoadContinuerController subject;

  @Test
  void roadContinuer_should_return_presignedUrl() throws Exception {
    var resource = getClass().getResource("/geojson/quai-de-bourbon.geojson");
    assertNotNull(resource);
    var file = new File(resource.toURI());
    String geojsonInput = Files.readString(file.toPath(), StandardCharsets.UTF_8);

    Integer zoom = 17;
    Integer imageSize = 1_024;

    var tilingConf = new TilingConf(zoom, imageSize);
    var expected = Map.of("url", "https://moked/qua-de-bourbon-continued.geojson");

    when(roadContinuerService.getTilingConf(zoom, imageSize)).thenReturn(tilingConf);
    when(roadContinuerService.continueRoute(geojsonInput, tilingConf)).thenReturn(expected);

    var actual = subject.roadContinuer(geojsonInput, zoom, imageSize);

    assertEquals(expected, actual);
    verify(roadContinuerService).getTilingConf(zoom, imageSize);
    verify(roadContinuerService).continueRoute(geojsonInput, tilingConf);
  }
}
