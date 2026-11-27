package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.file.FileWriter.createTempDirectory;
import static app.bpartners.geojobs.service.event.GeoJsonConversionTaskConsumer.GEO_JSON_BUCKET_FOLDER;
import static app.bpartners.geojobs.service.event.GeoJsonConversionTaskConsumer.GEO_JSON_EXTENSION;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.GeoJsonConversionAssemblyInitiated;
import app.bpartners.geojobs.endpoint.event.model.GeoJsonConversionAssemblySucceeded;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.GeoJsonConversionJobRepository;
import app.bpartners.geojobs.repository.GeoJsonConversionTaskRepository;
import app.bpartners.geojobs.repository.model.geojson.GeoJsonConversionTask;
import app.bpartners.geojobs.service.DetectionService;
import app.bpartners.geojobs.service.GeoFeatureConverter;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.service.geojson.GeoJson;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
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
  private final DetectionService detectionService;
  private final ZipGeoJsonAssembler zipGeoJsonAssembler;
  private final GeoFeatureConverter geoFeatureConverter;

  @Override
  public void accept(GeoJsonConversionAssemblyInitiated event) {
    var conversionJobId = event.getGeoJsonConversionJobId();
    var geoJsonConversionJob =
        geoJsonConversionJobRepository.findById(conversionJobId).orElseThrow();
    var zoneDetectionJob =
        zoneDetectionJobService.findById(geoJsonConversionJob.getZoneDetectionJobId());
    var detection = detectionService.getByZoneDetectionJob(zoneDetectionJob);
    if (detection != null && detection.isOutputZipped()) {
      zipGeoJsonAssembler.accept(geoJsonConversionJob);
      return;
    }
    var conversionTasks = geoJsonConversionTaskRepository.findAllByJobId(conversionJobId);
    var geoFeaturesList = getGeoFeaturesList(conversionTasks);
    var geoJson = new GeoJson(geoFeaturesList);
    var outputFileName = zoneDetectionJob.getZoneName().replace(" ", "_") + GEO_JSON_EXTENSION;
    var geoJsonFinalFile =
        fileWriter.write(
            geoJson.getStringValue().getBytes(StandardCharsets.UTF_8),
            createTempDirectory(),
            outputFileName);

    var combinedFileKey = GEO_JSON_BUCKET_FOLDER + zoneDetectionJob.getId() + "/" + outputFileName;

    bucketComponent.upload(geoJsonFinalFile, combinedFileKey);

    var savedConversionJob =
        geoJsonConversionJobRepository.save(
            geoJsonConversionJob.toBuilder().fileKey(combinedFileKey).build());
    if (zoneDetectionJob.isFinished()) {
      if (detection != null) {
        detectionRepository.save(
            detection.toBuilder().geojsonS3FileKey(savedConversionJob.getFileKey()).build());
      }
      eventProducer.accept(
          List.of(
              GeoJsonConversionAssemblySucceeded.builder()
                  .geoJsonConversionJob(savedConversionJob)
                  .build()));
    }
  }

  private List<GeoJson.GeoFeature> getGeoFeaturesList(List<GeoJsonConversionTask> conversionTasks) {
    var partialConvertedGeoJsonFiles =
        conversionTasks.stream()
            .map(conversionTask -> bucketComponent.download(conversionTask.getFileKey()))
            .toList();
    return geoFeatureConverter.apply(partialConvertedGeoJsonFiles);
  }
}
