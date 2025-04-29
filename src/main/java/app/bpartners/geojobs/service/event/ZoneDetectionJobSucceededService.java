package app.bpartners.geojobs.service.event;

import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationDeliveryJobRequested;
import app.bpartners.geojobs.endpoint.event.model.zone.ZoneDetectionJobSucceeded;
import app.bpartners.geojobs.repository.AnnotationDeliveryConfigurationRepository;
import app.bpartners.geojobs.repository.DetectableObjectConfigurationRepository;
import app.bpartners.geojobs.repository.MachineDetectedTileRepository;
import app.bpartners.geojobs.service.DetectionFinishedMailer;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.service.geojson.GeoJsonConversionJobService;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
  private final MachineDetectedTileRepository machineDetectedTileRepository;
  private final DetectableObjectConfigurationRepository detectableObjectConfigurationRepository;
  private final DetectionFinishedMailer detectionFinishedMailer;

  @Override
  @Transactional
  public void accept(ZoneDetectionJobSucceeded event) {
    var succeededJobId = event.getSucceededJobId();
    var succeededZoneDetectionJob = zoneDetectionJobService.findById(succeededJobId);
    if (zoneDetectionJobService.countInDoubtDetectedTileToDeliveryById(succeededJobId) == 0L) {
      geoJsonConversionJobService.getOrComputeGeoJsonConversionJob(succeededZoneDetectionJob);
      return;
    }
    var detectableObjectConfigurations =
        detectableObjectConfigurationRepository.findAllByDetectionJobId(succeededJobId);
    boolean machineDetectionFoundAnyDetectedTileFromDetectableConfiguration =
        detectableObjectConfigurations.stream()
            .anyMatch(
                detectableConfiguration ->
                    machineDetectedTileRepository.countByZdjJobIdAndDetectableType(
                            succeededJobId, detectableConfiguration.getObjectType().name())
                        > 0);
    if (!machineDetectionFoundAnyDetectedTileFromDetectableConfiguration) {
      var succeededDatetime = succeededZoneDetectionJob.getStatus().getCreationDatetime();
      var zoneName = succeededZoneDetectionJob.getZoneName();
      var formattedCreationDatetime =
          DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
              .format(succeededDatetime.atZone(ZoneId.of("Europe/Paris")));

      var emailSubject =
          String.format(
              "Analyse sur la zone %s terminée le %s", zoneName, formattedCreationDatetime);
      detectionFinishedMailer.accept(succeededZoneDetectionJob.getEmailReceiver(), emailSubject);
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
