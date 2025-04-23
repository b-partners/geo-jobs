package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.DetectionAddressConversionJobStatusChanged;
import app.bpartners.geojobs.endpoint.event.model.DetectionSaved;
import app.bpartners.geojobs.repository.DetectionAddressConversionTaskRepository;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.DetectionAddressConversionJob;
import app.bpartners.geojobs.repository.model.DetectionAddressConversionTask;
import app.bpartners.geojobs.service.GeoServerLayerRetriever;
import app.bpartners.geojobs.service.StatusChangedHandler;
import app.bpartners.geojobs.service.ZoneService;
import app.bpartners.geojobs.service.geoserver.GeoServerConfiguration;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DetectionAddressConversionJobStatusChangedService
    implements Consumer<DetectionAddressConversionJobStatusChanged> {
  private final StatusChangedHandler statusChangedHandler;
  private final DetectionRepository detectionRepository;
  private final DetectionAddressConversionTaskRepository detectionAddressConversionTaskRepository;
  private final EventProducer eventProducer;
  private final GeoServerConfiguration geoServerConfiguration;
  private final GeoServerLayerRetriever layerRetriever;
  private final ZoneService zoneService;

  @Override
  public void accept(DetectionAddressConversionJobStatusChanged event) {
    var oldJob = event.getOldJob();
    var newJob = event.getNewJob();

    var onSucceededStatusChangedHandler =
        new OnSucceededStatusChangedHandler(
            newJob,
            detectionRepository,
            detectionAddressConversionTaskRepository,
            eventProducer,
            geoServerConfiguration,
            layerRetriever,
            zoneService);
    var onFailedStatusChangedHandler = new OnFailedStatusChangedHandler(newJob);

    statusChangedHandler.handle(
        event,
        newJob.getStatus(),
        oldJob.getStatus(),
        onSucceededStatusChangedHandler,
        onFailedStatusChangedHandler);
  }

  private record OnSucceededStatusChangedHandler(
      DetectionAddressConversionJob newJob,
      DetectionRepository detectionRepository,
      DetectionAddressConversionTaskRepository detectionAddressConversionTaskRepository,
      EventProducer eventProducer,
      GeoServerConfiguration geoServerConfiguration,
      GeoServerLayerRetriever layerRetriever,
      ZoneService zoneService)
      implements Runnable {

    @Override
    public void run() {
      var detectionId = newJob.getDetectionId();
      var tasks = detectionAddressConversionTaskRepository.findAllByJobId(newJob.getId());
      var convertedFeatures =
          tasks.stream().map(DetectionAddressConversionTask::getFeature).toList();
      var layer = layerRetriever.apply(tasks);
      var detection = detectionRepository.findById(detectionId).orElseThrow();

      var savedDetection =
          detectionRepository.save(
              detection.toBuilder()
                  .multiPolygonGeoJsonZone(convertedFeatures)
                  .geoServerProperties(geoServerConfiguration.defaultGeoServerProperties(layer))
                  .build());

      eventProducer.accept(List.of(DetectionSaved.builder().detection(savedDetection).build()));

      zoneService.processDetectionSteps(savedDetection);
    }
  }

  private record OnFailedStatusChangedHandler(DetectionAddressConversionJob newJob)
      implements Runnable {

    @Override
    public void run() {
      // TODO : notify be email for example
      log.info("Failed to convert detection.id={} addresses into geojson", newJob.getId());
    }
  }
}
