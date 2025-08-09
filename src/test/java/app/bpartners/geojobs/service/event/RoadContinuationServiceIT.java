package app.bpartners.geojobs.service.event;

import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.model.RoadContinuationRequested;
import app.bpartners.geojobs.service.RoadContinuerService;
import java.io.File;
import org.junit.jupiter.api.Test;

class RoadContinuationServiceIT {

  @Test
  void accept_should_call_continueRouteAsync() {
    RoadContinuerService roadContinuerService = mock(RoadContinuerService.class);
    RoadContinuationService service = new RoadContinuationService(roadContinuerService);
    RoadContinuationRequested event = mock(RoadContinuationRequested.class);
    File fakeFile = mock(File.class);

    when(event.getGeoJSON()).thenReturn(fakeFile);

    service.accept(event);

    verify(roadContinuerService, times(1))
        .continueRouteAsync(eq(fakeFile), any(), any(), anyString());
  }
}
