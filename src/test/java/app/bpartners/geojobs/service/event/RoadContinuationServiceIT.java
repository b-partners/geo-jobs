package app.bpartners.geojobs.service.event;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.model.RoadContinuationRequested;
import app.bpartners.geojobs.repository.GeoJsonRoadContinuationRepository;
import app.bpartners.geojobs.repository.model.geojson.GeoJsonRoadContinuation;
import app.bpartners.geojobs.service.RoadContinuerService;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RoadContinuationServiceIT {

  @Test
  void accept_should_save_and_call_continueRoute() throws IOException {
    // Mocks
    RoadContinuerService roadContinuerService = mock(RoadContinuerService.class);
    GeoJsonRoadContinuationRepository continuationRepository =
        mock(GeoJsonRoadContinuationRepository.class);
    RoadContinuationRequested event = mock(RoadContinuationRequested.class);
    File fakeFile = mock(File.class);

    when(event.getGeoJSON()).thenReturn(fakeFile);
    when(fakeFile.getAbsolutePath()).thenReturn("/tmp/fake.geojson");
    when(roadContinuerService.continueRoute(any(File.class), any(), any()))
        .thenReturn(Map.of("url", "http://fake-url"));

    RoadContinuationService service =
        new RoadContinuationService(roadContinuerService, continuationRepository);

    service.accept(event);

    verify(continuationRepository, atLeastOnce()).save(any(GeoJsonRoadContinuation.class));
    verify(roadContinuerService, times(1)).continueRoute(eq(fakeFile), isNull(), isNull());
  }
}
