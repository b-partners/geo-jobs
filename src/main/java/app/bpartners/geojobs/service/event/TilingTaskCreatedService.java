package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.gen.DetectionJobCreated;
import app.bpartners.geojobs.endpoint.event.gen.TilingTaskCreated;
import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.file.BucketComponent;
import app.bpartners.geojobs.file.BucketConf;
import app.bpartners.geojobs.file.FileUnzipper;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.repository.model.geo.Parcel;
import app.bpartners.geojobs.repository.model.geo.tiling.Tile;
import app.bpartners.geojobs.repository.model.geo.tiling.TilingTask;
import app.bpartners.geojobs.service.geo.tiling.TilesDownloader;
import app.bpartners.geojobs.service.geo.tiling.TilingTaskStatusService;
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
  private final BucketConf bucketConf;
  private final TilingTaskStatusService tilingTaskStatusService;
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
      tilingTaskStatusService.fail(task);
      throw new ApiException(SERVER_EXCEPTION, e);
    }

    eventProducer.accept(List.of(new DetectionJobCreated(task)));
    tilingTaskStatusService.succeed(task);
  }

  private void setParcelTiles(File tilesDir, Parcel parcel, String bucketKey) {
    parcel.setTiles(getParcelTiles(new ArrayList<>(), tilesDir, parcel, bucketKey));
  }

  private List<Tile> getParcelTiles(
      List<Tile> accumulator, File tilesFile, Parcel parcel, String bucketKey) {
    if (!tilesFile.isDirectory()) {
      var enrichedAccumulator = new ArrayList<>(accumulator);

      String entryParentPath = tilesFile.getPath();
      String[] dir = entryParentPath.split("/");
      var x = Integer.valueOf(dir[dir.length - 2]);
      var z = Integer.valueOf(dir[dir.length - 3]);
      var y = Integer.valueOf(FileUnzipper.stripExtension(tilesFile.getName()));
      String bucketName = bucketConf.getBucketName();
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
              .creationDatetime(now().toString())
              .coordinates(new TileCoordinates().x(x).y(y).z(z))
              .bucketPath(bucketName + "/" + bucketKey + filePath)
              .build());

      return enrichedAccumulator;
    }

    return Arrays.stream(tilesFile.listFiles())
        .flatMap(subFile -> getParcelTiles(accumulator, subFile, parcel, bucketKey).stream())
        .collect(Collectors.toList());
  }
}
