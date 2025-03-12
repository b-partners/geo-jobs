package app.bpartners.geojobs.service.event;

import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationDeliveryJobRequested;
import app.bpartners.geojobs.endpoint.event.model.zone.ZoneDetectionJobSucceeded;
import app.bpartners.geojobs.repository.AnnotationDeliveryConfigurationRepository;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.service.geojson.GeoJsonConversionJobService;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class ZoneDetectionJobSucceededService implements Consumer<ZoneDetectionJobSucceeded> {
  private final AnnotationDeliveryConfigurationRepository annotationDeliveryConfigurationRepository;
  private final ZoneDetectionJobService zoneDetectionJobService;
  private final GeoJsonConversionJobService geoJsonConversionJobService;
  private final EventProducer eventProducer;

  @Override
  @Transactional
  public void accept(ZoneDetectionJobSucceeded event) {
    var succeededJobId = event.getSucceededJobId();
    if (zoneDetectionJobService.countInDoubtDetectedTileToDeliveryById(succeededJobId) == 0L) {
      geoJsonConversionJobService.getOrComputeGeoJsonConversionJob(
          zoneDetectionJobService.findById(succeededJobId));
      return;
    }
    var minimumConfidenceForDelivery =
        annotationDeliveryConfigurationRepository
            .findLatestConfiguration()
            .orElseThrow(
                () -> new IllegalStateException("No annotation delivery configuration found"))
            .getMinimumConfidenceForDelivery();
    var annotationJobWithObjectsIdTruePositive = randomUUID().toString();
    var annotationJobWithObjectsIdFalsePositive = randomUUID().toString();
    var annotationJobWithoutObjectsId = randomUUID().toString();
    eventProducer.accept(
        List.of(
            AnnotationDeliveryJobRequested.builder()
                .jobId(succeededJobId)
                .minimumConfidenceForDelivery(minimumConfidenceForDelivery)
                .annotationJobWithObjectsIdTruePositive(annotationJobWithObjectsIdTruePositive)
                .annotationJobWithObjectsIdFalsePositive(annotationJobWithObjectsIdFalsePositive)
                .annotationJobWithoutObjectsId(annotationJobWithoutObjectsId)
                .build()));
  }
}
