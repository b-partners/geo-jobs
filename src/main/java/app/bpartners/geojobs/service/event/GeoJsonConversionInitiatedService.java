package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static java.nio.file.attribute.PosixFilePermissions.asFileAttribute;
import static java.nio.file.attribute.PosixFilePermissions.fromString;
import static java.time.temporal.ChronoUnit.FOREVER;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.model.GeoJsonConversionInitiated;
import app.bpartners.geojobs.file.BucketComponent;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.repository.HumanDetectionJobRepository;
import app.bpartners.geojobs.repository.model.detection.DetectedTile;
import app.bpartners.geojobs.repository.model.detection.HumanDetectionJob;
import app.bpartners.geojobs.service.geojson.GeoJsonConversionTaskService;
import app.bpartners.geojobs.service.geojson.GeoJsonConversionTaskStatusService;
import app.bpartners.geojobs.service.geojson.GeoJsonConverter;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class GeoJsonConversionInitiatedService implements Consumer<GeoJsonConversionInitiated> {
  private static final String TEMP_FOLDER_PERMISSION = "rwx------";
  private static final String GEO_JSON_EXTENSION = ".geojson";
  private final HumanDetectionJobRepository humanDetectionJobRepository;
  private final GeoJsonConversionTaskStatusService taskStatusService;
  private final GeoJsonConversionTaskService taskService;
  private final GeoJsonConverter geoJsonConverter;
  private final FileWriter writer;
  private final BucketComponent bucketComponent;

  @Override
  public void accept(GeoJsonConversionInitiated initiated) {
    var jobId = initiated.getJobId();
    var taskId = initiated.getConversionTaskId();
    var zoneName = initiated.getZoneName();
    var task = taskService.getById(taskId);
    taskStatusService.process(task);
    var fileKey = jobId + "/" + zoneName + GEO_JSON_EXTENSION;
    List<DetectedTile> detectedTile =
        humanDetectionJobRepository.findByZoneDetectionJobId(jobId).stream()
            .map(HumanDetectionJob::getDetectedTiles)
            .flatMap(List::stream)
            .toList(); // Should be replaced by detected tiles after human verification
    try {
      var geoJson = geoJsonConverter.convert(detectedTile);
      var geoJsonAsByte = writer.writeAsByte(geoJson);
      var geoJsonAsFile =
          writer.write(geoJsonAsByte, createTempDirectory(), zoneName + GEO_JSON_EXTENSION);
      bucketComponent.upload(geoJsonAsFile, fileKey);
      var url = bucketComponent.presign(fileKey, FOREVER.getDuration());
      task.setGeoJsonUrl(url.toString());
      var finished = taskStatusService.succeed(task);
      taskService.save(finished);
    } catch (RuntimeException e) {
      var failed = taskStatusService.fail(task);
      taskService.save(failed);
      throw new ApiException(SERVER_EXCEPTION, e.getMessage());
    }
  }

  @SneakyThrows
  public File createTempDirectory() {
    Path tempDir =
        Files.createTempDirectory(
            randomUUID().toString(), asFileAttribute(fromString(TEMP_FOLDER_PERMISSION)));
    var dirFile = tempDir.toFile();
    dirFile.deleteOnExit();
    return dirFile;
  }
}
