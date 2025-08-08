package app.bpartners.geojobs.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.rest.postprocessing.Geojson;
import app.bpartners.geojobs.service.geojson.GeoJsonContinuerService;
import java.io.File;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;

public class GeoJsonContinuerControllerIT {
  private final GeoJsonContinuerService geoJsonContinuerServiceMock =
      Mockito.mock(GeoJsonContinuerService.class);
  private final GeoJsonContinuerController controller =
      new GeoJsonContinuerController(geoJsonContinuerServiceMock);
    private final Integer imgSize = 1_024;
    private final Integer zoom = 17;

  @Test
  void continueGeoJson_shouldReturnPresignedUrl() throws Exception {

    MockMultipartFile file =
        new MockMultipartFile("file", "input.geojson", "application/json", "{}".getBytes());

    var mockResult = mock(Geojson.class);

    when(geoJsonContinuerServiceMock.continueGeojson(any(File.class),imgSize,zoom)).thenReturn(mockResult);
    when(geoJsonContinuerServiceMock.generatePresignedUrl(file,imgSize,zoom))
        .thenReturn("https://url-presigned");

    String resultUrl = controller.continueGeoJson(file,imgSize,zoom);

    verify(geoJsonContinuerServiceMock).generatePresignedUrl(file,imgSize,zoom);

    assertEquals("https://url-presigned", resultUrl);
  }
}
