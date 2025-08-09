package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.RoadContinuationRequested;
import app.bpartners.geojobs.repository.GeoJsonRoadContinuationRepository;
import app.bpartners.geojobs.repository.model.geojson.GeoJsonRoadContinuation;
import app.bpartners.geojobs.repository.model.geojson.RoadContinuationProcessStatus;
import app.bpartners.geojobs.service.RoadContinuerService;
import java.io.File;
import java.io.IOException;
import java.util.Map;
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
  private final GeoJsonRoadContinuationRepository continuationRepository;

  @Override
  public void accept(RoadContinuationRequested event) {
    File geoJsonFile = event.getGeoJSON();
    String continuationId = UUID.randomUUID().toString();

    log.info(
        "Reçu RoadContinuationRequested, démarrage de la continuation asynchrone (id={})",
        continuationId);

    GeoJsonRoadContinuation record = new GeoJsonRoadContinuation();
    record.setId(continuationId);
    record.setOriginalGeoJsonPath(geoJsonFile.getAbsolutePath());
    record.setImageZoom(event.getZoom());
    record.setImageSize(event.getImageSize());
    record.setStatus(RoadContinuationProcessStatus.PROCESSING);

    continuationRepository.save(record);

    Map<String, String> result;
    try {
      result =
          roadContinuerService.continueRoute(geoJsonFile, event.getZoom(), event.getImageSize());
    } catch (IOException e) {
      log.error("Erreur lors de la continuation de la route (id={})", continuationId, e);
      throw new RuntimeException(e);
    }

    String presignedUrl = result.get("url");
    record.setContinuedGeoJsonPath(presignedUrl);
    record.setStatus(RoadContinuationProcessStatus.CONTINUED);
    continuationRepository.save(record);

    log.info("Continuation terminée (id={}, URL={})", continuationId, presignedUrl);
  }
}
