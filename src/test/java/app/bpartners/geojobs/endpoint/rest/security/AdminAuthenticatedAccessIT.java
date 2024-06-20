package app.bpartners.geojobs.endpoint.rest.security;

import static app.bpartners.geojobs.endpoint.rest.security.authentication.apikey.ApiKeyAuthenticator.APIKEY_HEADER_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.rest.api.DetectionApi;
import app.bpartners.geojobs.endpoint.rest.api.TilingApi;
import app.bpartners.geojobs.endpoint.rest.client.ApiClient;
import app.bpartners.geojobs.endpoint.rest.client.ApiException;
import app.bpartners.geojobs.endpoint.rest.controller.ZoneDetectionController;
import app.bpartners.geojobs.endpoint.rest.controller.ZoneTilingController;
import app.bpartners.geojobs.endpoint.rest.model.CreateZoneTilingJob;
import app.bpartners.geojobs.endpoint.rest.model.ZoneDetectionJob;
import app.bpartners.geojobs.endpoint.rest.model.ZoneTilingJob;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;

public class AdminAuthenticatedAccessIT extends FacadeIT {

  @LocalServerPort private int port;

  @Autowired ObjectMapper om;

  TilingApi tilingApi;
  DetectionApi detectionApi;

  @MockBean ZoneTilingController tilingController;
  @MockBean ZoneDetectionController detectionController;

  @BeforeEach
  void setUp() {
    var authenticatedClient = new ApiClient();
    authenticatedClient.setRequestInterceptor(
        builder -> builder.header(APIKEY_HEADER_NAME, "the-admin-api-key"));
    authenticatedClient.setScheme("http");
    authenticatedClient.setPort(port);
    authenticatedClient.setObjectMapper(om);

    tilingApi = new TilingApi(authenticatedClient);
    detectionApi = new DetectionApi(authenticatedClient);
  }

  @Test
  void admin_can_tile() throws ApiException {
    var expected = new ZoneTilingJob();
    when(tilingController.tileZone(any())).thenReturn(expected);

    var actual = tilingApi.tileZone(mock(CreateZoneTilingJob.class));

    assertEquals(expected, actual);
  }

  @Test
  void admin_can_detect() throws ApiException {
    var expected = new ZoneDetectionJob();
    when(detectionController.processZDJ(any(), any())).thenReturn(expected);

    var actual = detectionApi.processZDJ("dummy", List.of());

    assertEquals(expected, actual);
  }
}
