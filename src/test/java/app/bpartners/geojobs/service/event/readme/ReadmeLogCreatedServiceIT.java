package app.bpartners.geojobs.service.event.readme;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.rest.api.DetectionApi;
import app.bpartners.geojobs.endpoint.rest.client.ApiClient;
import app.bpartners.geojobs.endpoint.rest.client.ApiException;
import app.bpartners.geojobs.endpoint.rest.model.DetectionUsage;
import app.bpartners.geojobs.service.CommunityUsedSurfaceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;

import static app.bpartners.geojobs.conf.EnvConf.ADMIN_EMAIL;
import static app.bpartners.geojobs.endpoint.rest.model.DetectionSurfaceUnit.SQUARE_DEGREE;
import static app.bpartners.geojobs.endpoint.rest.security.authenticator.ApiKeyAuthenticator.API_KEY_HEADER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class ReadmeLogCreatedServiceIT extends FacadeIT {
  @LocalServerPort private int port;
  @Autowired ObjectMapper objectMapper;
  @MockBean CommunityUsedSurfaceService communityUsedSurfaceServiceMock;
  DetectionApi detectionApi;
  DetectionUsage expectedDetectionUsage = mock();

  @BeforeEach
  void setup(){
    var authenticatedClient = new ApiClient();
    authenticatedClient.setRequestInterceptor(
        builder -> builder.header(API_KEY_HEADER, ADMIN_EMAIL));
    authenticatedClient.setScheme("http");
    authenticatedClient.setPort(port);
    authenticatedClient.setObjectMapper(objectMapper);

    detectionApi = new DetectionApi(authenticatedClient);
    when(communityUsedSurfaceServiceMock.getUsage(any(), any())).thenReturn(expectedDetectionUsage);
  }

  @Test
  void create_readme_log_from_request_without_exception() throws ApiException {
    var actual = detectionApi.getDetectionUsage(SQUARE_DEGREE);

    assertEquals(expectedDetectionUsage, actual);
  }
}
