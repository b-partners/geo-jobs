package app.bpartners.geojobs.service.event;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.file.BucketComponent;
import app.bpartners.geojobs.file.FileUnzipper;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.repository.model.ParcelContent;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.repository.model.tiling.TilingTask;
import app.bpartners.geojobs.service.tiling.downloader.HttpApiTilesDownloader;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class TilingTaskConsumer implements Consumer<TilingTask> {
  private final HttpApiTilesDownloader httpApiTilesDownloader;
  private final BucketComponent bucketComponent;

  @Override
  public void accept(TilingTask tilingTask) {
    var parcel = tilingTask.getParcelContent();
    File downloadedTiles = httpApiTilesDownloader.apply(parcel);
    String bucketKey = downloadedTiles.getName();

    bucketComponent.upload(downloadedTiles, bucketKey);
    downloadedTiles.delete();

    setParcelTiles(downloadedTiles, parcel, bucketKey);
  }

  private void setParcelTiles(File tilesDir, ParcelContent parcelContent, String bucketKey) {
    parcelContent.setTiles(getParcelTiles(new ArrayList<>(), tilesDir, bucketKey));
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

  public static TilingTask withNewStatus(
      TilingTask task,
      Status.ProgressionStatus progression,
      Status.HealthStatus health,
      String message) {
    return (TilingTask)
        task.hasNewStatus(
            Status.builder()
                .progression(progression)
                .health(health)
                .creationDatetime(now())
                .message(message)
                .build());
  }
}
