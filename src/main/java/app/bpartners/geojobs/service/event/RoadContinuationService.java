package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.RoadContinuationRequested;
import app.bpartners.geojobs.service.RoadContinuerService;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class RoadContinuationService implements Consumer<RoadContinuationRequested> {

  private final RoadContinuerService roadContinuerService;

  @Override
  public void accept(RoadContinuationRequested event) {
    var geoJsonFile = event.getGeoJSON();
    String continuationId = UUID.randomUUID().toString();

    log.info(
        "Received RoadContinuationRequested event, starting async continuation for id={}",
        continuationId);
    roadContinuerService.continueRouteAsync(geoJsonFile, null, null, continuationId);
  }
}
