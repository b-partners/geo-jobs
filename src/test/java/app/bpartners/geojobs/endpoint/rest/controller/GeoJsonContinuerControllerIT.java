package app.bpartners.geojobs.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.conf.FacadeIT;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;

@Slf4j
public class GeoJsonContinuerControllerIT extends FacadeIT {
  @MockBean GeoJsonContinuerController subject;

  @Test
  void test_continue_geojson_controller() throws Exception {
    var input = "{}".getBytes();

    when(subject.continueGeoJson(input)).thenReturn("https://url-presigned");

    String resultUrl = subject.continueGeoJson(input);

    verify(subject).continueGeoJson(input);

    assertEquals("https://url-presigned", resultUrl);
  }
}
