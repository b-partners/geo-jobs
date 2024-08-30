package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static java.nio.file.attribute.PosixFilePermissions.asFileAttribute;
import static java.nio.file.attribute.PosixFilePermissions.fromString;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.model.GeoJsonConversionInitiated;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.FullDetectionRepository;
import app.bpartners.geojobs.repository.model.GeoJsonConversionTask;
import app.bpartners.geojobs.service.detection.HumanDetectedTileService;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.service.geojson.GeoJsonConversionTaskService;
import app.bpartners.geojobs.service.geojson.GeoJsonConversionTaskStatusService;
import app.bpartners.geojobs.service.geojson.GeoJsonConverter;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
  private final ZoneDetectionJobService detectionJobService;

  @Override
  @Transactional
  public void accept(GeoJsonConversionInitiated event) {
    var taskId = event.getConversionTaskId();
    var task = taskService.getById(taskId);
    taskStatusService.process(task);

    var fileKey = consumeTask(event, task);

    var finishedConversionTask =
        taskStatusService.succeed(task.toBuilder().fileKey(fileKey).build());
    taskService.save(finishedConversionTask);
  }

  @NonNull
  private String consumeTask(GeoJsonConversionInitiated event, GeoJsonConversionTask task) {
    try {
      var humanZDJId = event.getJobId();
      var zoneName = event.getZoneName();
      var machineZDJ = detectionJobService.getMachineZDJFromHumanZDJ(humanZDJId);
      var machineZDJId = machineZDJ.getId();
      var fullDetectionJob =
          fullDetectionRepository
              .findByZdjId(machineZDJId)
              .orElseThrow(
                  () ->
                      new NotFoundException(
                          "Any fullDetectionJob associated to ZDJ(type=MACHINE, id="
                              + machineZDJId
                              + ")"));
      var fileKey = fullDetectionJob.getId() + "/" + zoneName + GEO_JSON_EXTENSION;
      var humanDetectedTiles = humanDetectedTileService.getByJobId(humanZDJId);
      var geoJson = geoJsonConverter.convert(humanDetectedTiles);
      var geoJsonAsByte = writer.writeAsByte(geoJson);
      var geoJsonAsFile =
          writer.write(geoJsonAsByte, createTempDirectory(), zoneName + GEO_JSON_EXTENSION);
      bucketComponent.upload(geoJsonAsFile, fileKey);

      var savedFullDetection =
          fullDetectionRepository.save(
              fullDetectionJob.toBuilder().geojsonS3FileKey(fileKey).build());
      return savedFullDetection.getGeojsonS3FileKey();
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
