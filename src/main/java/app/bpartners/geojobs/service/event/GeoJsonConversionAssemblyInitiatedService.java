package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.service.event.GeoJsonConversionTaskConsumer.GEO_JSON_BUCKET_FOLDER;
import static app.bpartners.geojobs.service.event.GeoJsonConversionTaskConsumer.GEO_JSON_EXTENSION;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.GeoJsonConversionAssemblyInitiated;
import app.bpartners.geojobs.endpoint.event.model.GeoJsonConversionAssemblySucceeded;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.GeoJsonConversionJobRepository;
import app.bpartners.geojobs.repository.GeoJsonConversionTaskRepository;
import app.bpartners.geojobs.service.SubscriptionConsumptionService;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GeoJsonConversionAssemblyInitiatedService
    implements Consumer<GeoJsonConversionAssemblyInitiated> {
  private final GeoJsonConversionTaskRepository geoJsonConversionTaskRepository;
  private final GeoJsonConversionJobRepository geoJsonConversionJobRepository;
  private final BucketComponent bucketComponent;
  private final FileWriter fileWriter;
  private final ZoneDetectionJobService zoneDetectionJobService;
  private final DetectionRepository detectionRepository;
  private final EventProducer eventProducer;
  private final SubscriptionConsumptionService subscriptionConsumptionService;

  @Override
  public void accept(GeoJsonConversionAssemblyInitiated event) {
    var conversionJobId = event.getGeoJsonConversionJobId();
    var conversionTasks = geoJsonConversionTaskRepository.findAllByJobId(conversionJobId);
    var partialConvertedGeoJsonFiles =
        conversionTasks.stream()
            .map(conversionTask -> bucketComponent.download(conversionTask.getFileKey()))
            .toList();
    var geoJsonConversionJob =
        geoJsonConversionJobRepository.findById(conversionJobId).orElseThrow();
    var zoneDetectionJob =
        zoneDetectionJobService.findById(geoJsonConversionJob.getZoneDetectionJobId());
    var outputFileName = zoneDetectionJob.getZoneName() + "-final" + GEO_JSON_EXTENSION;
    var combinedConvertedGeoJsonFile =
        fileWriter.combineContent(partialConvertedGeoJsonFiles, outputFileName);
    var combinedFileKey = GEO_JSON_BUCKET_FOLDER + zoneDetectionJob.getId() + "/" + outputFileName;

    bucketComponent.upload(combinedConvertedGeoJsonFile, combinedFileKey);

    var savedConversionJob =
        geoJsonConversionJobRepository.save(
            geoJsonConversionJob.toBuilder().fileKey(combinedFileKey).build());

    var humanZDJ = zoneDetectionJobService.getHumanZdjFromZdjId(zoneDetectionJob.getId());
    var machineZDJ = zoneDetectionJobService.getMachineZdjFromZdjId(zoneDetectionJob.getId());
    var detection =
        detectionRepository
            .findByZdjId(humanZDJ.getId())
            .orElseGet(
                () -> {
                  var optionalDetectionFromMachineZDJ =
                      detectionRepository.findByZdjId(machineZDJ.getId());
                  if (optionalDetectionFromMachineZDJ.isPresent()) {
                    return optionalDetectionFromMachineZDJ.orElseThrow();
                  }
                  throw new NotFoundException(
                      "Any detection found associated to ZDJ(id=" + zoneDetectionJob.getId() + ")");
                });
    var saved =
        detectionRepository.save(
            detection.toBuilder().geojsonS3FileKey(savedConversionJob.getFileKey()).build());
    eventProducer.accept(
        List.of(
            GeoJsonConversionAssemblySucceeded.builder()
                .geoJsonConversionJob(savedConversionJob)
                .build()));
    subscriptionConsumptionService.sendSubscriptionConsumption(saved.getCommunityOwnerId());
  }
}
