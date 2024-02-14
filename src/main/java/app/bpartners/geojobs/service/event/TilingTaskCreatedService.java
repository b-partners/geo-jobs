package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.FAILED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.gen.TilingTaskCreated;
import app.bpartners.geojobs.endpoint.event.gen.TilingTaskFailed;
import app.bpartners.geojobs.endpoint.event.gen.TilingTaskSucceeded;
import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.file.BucketConfiguration;
import app.bpartners.geojobs.file.FileUnzipper;
import app.bpartners.geojobs.file.self.BucketComponent;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.job.service.RetryableTaskStatusService;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.repository.model.tiling.TilingTask;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.tiling.TilesDownloader;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class TilingTaskCreatedService implements Consumer<TilingTaskCreated> {
  private final TilesDownloader tilesDownloader;
  private final BucketComponent bucketComponent;
  private final BucketConfiguration bucketConf;
  private final RetryableTaskStatusService<TilingTask, ZoneTilingJob> tilingTaskStatusService;
  private final EventProducer eventProducer;

  @Override
  public void accept(TilingTaskCreated tilingTaskCreated) {
    TilingTask task = tilingTaskCreated.getTask();
    tilingTaskStatusService.process(task);

    try {
      File downloadedTiles = tilesDownloader.apply(tilingTaskCreated.getTask().getParcel());
      String bucketKey = downloadedTiles.getName();
      bucketComponent.upload(downloadedTiles, bucketKey);
      setParcelTiles(downloadedTiles, task.getParcel(), bucketKey);
    } catch (Exception e) {
      task.hasNewStatus(
          Status.builder().progression(FINISHED).health(FAILED).creationDatetime(now()).build());
      eventProducer.accept(List.of(new TilingTaskFailed(task, 1)));
      return;
    }

    task.hasNewStatus(
        Status.builder().progression(FINISHED).health(SUCCEEDED).creationDatetime(now()).build());
    eventProducer.accept(List.of(new TilingTaskSucceeded(task)));
  }

  private void setParcelTiles(File tilesDir, Parcel parcel, String bucketKey) {
    parcel.setTiles(getParcelTiles(new ArrayList<>(), tilesDir, bucketKey));
  }

  private List<Tile> getParcelTiles(List<Tile> accumulator, File tilesFile, String bucketKey) {
    if (!tilesFile.isDirectory()) {
      var enrichedAccumulator = new ArrayList<>(accumulator);

      String entryParentPath = tilesFile.getPath();
      String[] dir = entryParentPath.split("/");
      var x = Integer.valueOf(dir[dir.length - 2]);
      var z = Integer.valueOf(dir[dir.length - 3]);
      var y = Integer.valueOf(FileUnzipper.stripExtension(tilesFile.getName()));
      String[] segments = entryParentPath.split("/");
      String filePath = "";

      if (segments.length >= 3) {
        filePath =
            "/"
                + segments[segments.length - 3]
                + "/"
                + segments[segments.length - 2]
                + "/"
                + segments[segments.length - 1];
      }

      enrichedAccumulator.add(
          Tile.builder()
              .id(randomUUID().toString())
              .creationDatetime(now())
              .coordinates(new TileCoordinates().x(x).y(y).z(z))
              .bucketPath(bucketKey + filePath)
              .build());

      return enrichedAccumulator;
    }

    return Arrays.stream(tilesFile.listFiles())
        .flatMap(subFile -> getParcelTiles(accumulator, subFile, bucketKey).stream())
        .collect(Collectors.toList());
  }
}
