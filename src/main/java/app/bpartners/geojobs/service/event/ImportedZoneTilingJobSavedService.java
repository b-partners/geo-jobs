package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.geojobs.repository.model.GeoJobType.TILING;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.gen.ImportedZoneTilingJobSaved;
import app.bpartners.geojobs.endpoint.rest.model.GeoServerParameter;
import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.file.BucketCustomizedComponent;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.repository.TilingTaskRepository;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.ParcelContent;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.repository.model.tiling.TilingTask;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.tiling.ZoneTilingJobService;
import java.net.URL;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.S3Object;

@Service
@AllArgsConstructor
public class ImportedZoneTilingJobSavedService implements Consumer<ImportedZoneTilingJobSaved> {
  private final BucketCustomizedComponent bucketCustomizedComponent;
  private final ZoneTilingJobService tilingJobService;
  private final TilingTaskRepository tilingTaskRepository;

  @Override
  public void accept(ImportedZoneTilingJobSaved importedZoneTilingJobSaved) {
    var job = tilingJobService.findById(importedZoneTilingJobSaved.getJobId());
    var bucketPath = importedZoneTilingJobSaved.getBucketPathKey();
    var geoServerParameter = importedZoneTilingJobSaved.getGeoServerParameter();
    var geoServerUrlValue = importedZoneTilingJobSaved.getGeoServerUrl();
    var s3Objects = bucketCustomizedComponent.listObjects(bucketPath);
    var tilingTasks =
        s3Objects.stream()
            .map(s3Object -> getFinishedTasks(s3Object, job, geoServerParameter, geoServerUrlValue))
            .toList();
    tilingTaskRepository.saveAll(tilingTasks);
    tilingJobService.recomputeStatus(job);
  }

  @SneakyThrows
  private TilingTask getFinishedTasks(
      S3Object s3Object,
      ZoneTilingJob job,
      GeoServerParameter geoServerParameter,
      String geoServerUrlValue) {
    String jobId = job.getId();
    URL geoServerUrl = new URL(geoServerUrlValue);
    String taskId = randomUUID().toString();
    String bucketPathKey = s3Object.key();
    return TilingTask.builder()
        .id(taskId)
        .jobId(jobId)
        .statusHistory(
            List.of(
                TaskStatus.builder()
                    .id(randomUUID().toString())
                    .taskId(taskId)
                    .jobType(TILING)
                    .health(SUCCEEDED)
                    .progression(FINISHED)
                    .creationDatetime(now())
                    .build()))
        .submissionInstant(now())
        .parcels(
            List.of(
                Parcel.builder()
                    .id(randomUUID().toString())
                    .parcelContent(
                        ParcelContent.builder()
                            .id(randomUUID().toString())
                            .feature(null) // TODO: distinct for each parcels
                            .creationDatetime(now())
                            .geoServerParameter(geoServerParameter)
                            .geoServerUrl(geoServerUrl)
                            .tiles(
                                List.of(
                                    Tile.builder()
                                        .id(randomUUID().toString())
                                        .bucketPath(bucketPathKey)
                                        .coordinates(fromBucketPathKey(bucketPathKey))
                                        .creationDatetime(now())
                                        .build()))
                            .build())
                    .build()))
        .build();
  }

  private TileCoordinates fromBucketPathKey(String bucketPathKey) {
    String[] bucketPathValues = bucketPathKey.split("/");
    if (bucketPathValues.length != 4) {
      throw new ApiException(
          SERVER_EXCEPTION,
          "Unable to convert bucketPathKey " + bucketPathKey + " to TilesCoordinates");
    }
    return new TileCoordinates()
        .x(Integer.valueOf(bucketPathValues[2]))
        .y(Integer.valueOf(bucketPathValues[3]))
        .z(Integer.valueOf(bucketPathValues[1]));
  }
}
