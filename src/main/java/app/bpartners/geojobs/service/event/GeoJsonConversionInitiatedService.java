package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static java.nio.file.attribute.PosixFilePermissions.asFileAttribute;
import static java.nio.file.attribute.PosixFilePermissions.fromString;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.model.GeoJsonConversionInitiated;
import app.bpartners.geojobs.file.BucketComponent;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.repository.FullDetectionRepository;
import app.bpartners.geojobs.repository.model.detection.HumanDetectedTile;
import app.bpartners.geojobs.service.detection.HumanDetectedTileService;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class GeoJsonConversionInitiatedService implements Consumer<GeoJsonConversionInitiated> {
  private static final String TEMP_FOLDER_PERMISSION = "rwx------";
  private static final String GEO_JSON_EXTENSION = ".geojson";
  private final HumanDetectedTileService humanDetectedTileService;
  private final GeoJsonConversionTaskStatusService taskStatusService;
  private final GeoJsonConversionTaskService taskService;
  private final GeoJsonConverter geoJsonConverter;
  private final FileWriter writer;
  private final BucketComponent bucketComponent;
  private final FullDetectionRepository fullDetectionRepository;

  @Override
  public void accept(GeoJsonConversionInitiated initiated) {
    var jobId = initiated.getJobId();
    var taskId = initiated.getConversionTaskId();
    var zoneName = initiated.getZoneName();
    var task = taskService.getById(taskId);
    var fullDetectionJob = fullDetectionRepository.findByZdjId(jobId);
    taskStatusService.process(task);
    var fileKey = jobId + "/" + zoneName + GEO_JSON_EXTENSION;
    List<HumanDetectedTile> humanDetectedTiles = humanDetectedTileService.getByJobId(jobId);
    try {
      var geoJson = geoJsonConverter.convert(humanDetectedTiles);
      var geoJsonAsByte = writer.writeAsByte(geoJson);
      var geoJsonAsFile =
          writer.write(geoJsonAsByte, createTempDirectory(), zoneName + GEO_JSON_EXTENSION);
      bucketComponent.upload(geoJsonAsFile, fileKey);
      task.setFileKey(fileKey);
      var finished = taskStatusService.succeed(task);
      taskService.save(finished);
      if (fullDetectionJob.isPresent()) {
        var persisted = fullDetectionJob.get();
        persisted.setGeojsonS3FileKey(fileKey);
        fullDetectionRepository.save(persisted);
      }
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
