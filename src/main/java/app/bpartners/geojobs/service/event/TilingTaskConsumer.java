package app.bpartners.geojobs.service.event;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.file.zip.FileUnzipper;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.repository.model.ParcelContent;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.repository.model.tiling.TilingTask;
import app.bpartners.geojobs.service.tiling.downloader.TilesDownloader;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
@Slf4j
public class TilingTaskConsumer implements Consumer<TilingTask> {
  private final TilesDownloader tilesDownloader;
  private final BucketComponent bucketComponent;

  @Override
  public void accept(TilingTask tilingTask) {
    var parcel = tilingTask.getParcelContent();
    File downloadedTiles = tilesDownloader.apply(parcel);
    String bucketKey = downloadedTiles.getName();

    bucketComponent.upload(downloadedTiles, bucketKey);
    boolean isDeleted = downloadedTiles.delete();
    if (!isDeleted) {
      System.out.println(
          "Failed to delete downloaded tiles file: " + downloadedTiles.getAbsolutePath());
    }

    setParcelTiles(downloadedTiles, parcel, bucketKey);
  }

  private void setParcelTiles(File tilesDir, ParcelContent parcelContent, String bucketKey) {
    List<Tile> extractedTiles = getParcelTiles(new ArrayList<>(), tilesDir, bucketKey);
    parcelContent.setTiles(extractedTiles);
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

      Tile extractedTile =
          Tile.builder()
              .id(randomUUID().toString())
              .creationDatetime(now())
              .coordinates(new TileCoordinates().x(x).y(y).z(z))
              .bucketPath(bucketKey + filePath)
              .build();
      enrichedAccumulator.add(extractedTile);

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
