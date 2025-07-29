package app.bpartners.geojobs.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.conf.FacadeIT;
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
class RoadContinuerControllerIT extends FacadeIT {

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
    var expected = Map.of("url", "https://moked/qua-de-bourbon-continued.geojson");

    when(roadContinuerService.continueRoute(geojsonInput, zoom, imageSize)).thenReturn(expected);
    var actual = subject.roadContinuer(geojsonInput, zoom, imageSize);

    assertEquals(expected, actual);
    verify(roadContinuerService).continueRoute(geojsonInput, zoom, imageSize);
  }
}
