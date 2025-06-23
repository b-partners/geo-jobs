package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.file.FileWriter.createTempDirectory;
import static app.bpartners.geojobs.service.event.GeoJsonConversionTaskConsumer.GEO_JSON_BUCKET_FOLDER;
import static app.bpartners.geojobs.service.event.GeoJsonConversionTaskConsumer.GEO_JSON_EXTENSION;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.GeoJsonConversionAssemblySucceeded;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.GeoJsonConversionJobRepository;
import app.bpartners.geojobs.repository.GeoJsonConversionTaskRepository;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import app.bpartners.geojobs.repository.model.geojson.GeoJsonConversionJob;
import app.bpartners.geojobs.repository.model.geojson.GeoJsonConversionTask;
import app.bpartners.geojobs.service.DetectionService;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ZipGeoJsonAssembler implements Consumer<GeoJsonConversionJob> {
  private final GeoJsonConversionTaskRepository geoJsonConversionTaskRepository;
  private final GeoJsonConversionJobRepository geoJsonConversionJobRepository;
  private final BucketComponent bucketComponent;
  private final ZoneDetectionJobService zoneDetectionJobService;
  private final DetectionRepository detectionRepository;
  private final DetectionService detectionService;
  private final EventProducer eventProducer;

  @Override
  public void accept(GeoJsonConversionJob geoJsonConversionJob) {
    var conversionJobId = geoJsonConversionJob.getId();
    var conversionTasks = geoJsonConversionTaskRepository.findAllByJobId(conversionJobId);
    var zoneDetectionJob =
        zoneDetectionJobService.findById(geoJsonConversionJob.getZoneDetectionJobId());
    var outputFileName = zoneDetectionJob.getZoneName() + "-final" + GEO_JSON_EXTENSION;
    var combinedFileKey = GEO_JSON_BUCKET_FOLDER + zoneDetectionJob.getId() + "/" + outputFileName;
    var detection = detectionService.getByZoneDetectionJob(zoneDetectionJob);

    var zipFile = computeZipFile(conversionTasks, outputFileName);

    bucketComponent.upload(zipFile, combinedFileKey);

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

  @SneakyThrows
  private File computeZipFile(List<GeoJsonConversionTask> conversionTasks, String outputFileName) {
    var suffix = ".zip";
    var prefix = outputFileName.replaceAll(".geojson", "");
    var zipFile = File.createTempFile(prefix, suffix, createTempDirectory());
    var taskGeoJsonMap = new HashMap<DetectableType, File>();
    conversionTasks.forEach(
        conversionTask ->
            taskGeoJsonMap.put(
                conversionTask.getDetectableType(),
                bucketComponent.download(conversionTask.getFileKey())));

    try (OutputStream fos = Files.newOutputStream(zipFile.toPath());
        ZipOutputStream zipOut = new ZipOutputStream(fos)) {
      taskGeoJsonMap.forEach(
          (key, value) -> {
            ZipEntry zipEntry = new ZipEntry(prefix + "_" + key.name() + ".geojson");
            try {
              zipOut.putNextEntry(zipEntry);
              zipOut.write(Files.readString(value.toPath()).getBytes());
              zipOut.closeEntry();
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          });
    }
    return zipFile;
  }
}
