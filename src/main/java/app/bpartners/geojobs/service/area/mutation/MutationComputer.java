package app.bpartners.geojobs.service.area.mutation;

import app.bpartners.geojobs.repository.model.ParcelContent;
import app.bpartners.geojobs.service.area.mutation.model.InstantTile;
import app.bpartners.geojobs.service.area.mutation.model.MutationContext;
import app.bpartners.geojobs.service.area.mutation.model.MutationType;
import app.bpartners.geojobs.service.tiling.downloader.TilesDownloader;
import java.io.File;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MutationComputer {
  private final MutationApi api;
  private final TilesDownloader tilesDownloader;

  public MutationType apply(MutationContext context) {
    var recentTileImageFile = tileImageFile(context.mostRecent());
    var oldTileImageFile = tileImageFile(context.older());

    var filename = "mutation_" + UUID.randomUUID();
    var mutationResponse =
        api.detectMutation(
            oldTileImageFile, recentTileImageFile, context.maskImageFile(), filename);

    return mutationResponse.mutation();
  }

  private File tileImageFile(InstantTile tile) {
    var parcelFeature = tile.parcelDelimitations().getFirst().feature();
    var imageSource = tile.imageSource();
    var parcelContent =
        ParcelContent.builder()
            .id(parcelFeature.getId())
            .feature(parcelFeature)
            .geoServerUrl(imageSource.apiUrl())
            .geoServerParameter(imageSource.geoServerParameter())
            .creationDatetime(tile.date())
            .build();
    return singleTileImage(tilesDownloader.apply(parcelContent));
  }

  private File singleTileImage(File downloadedTiles) {
    if (!downloadedTiles.isDirectory()) {
      return downloadedTiles;
    }
    var children = downloadedTiles.listFiles();
    if (children == null || children.length == 0) {
      throw new IllegalStateException("No tile image downloaded for parcel");
    }
    return singleTileImage(children[0]);
  }
}
