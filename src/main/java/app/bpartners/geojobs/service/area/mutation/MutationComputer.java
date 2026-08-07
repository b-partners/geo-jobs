package app.bpartners.geojobs.service.area.mutation;

import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.repository.model.ParcelContent;
import app.bpartners.geojobs.repository.model.detection.FeatureWithDelimitation;
import app.bpartners.geojobs.service.area.mutation.model.InstantParcel;
import app.bpartners.geojobs.service.area.mutation.model.MutationContext;
import app.bpartners.geojobs.service.area.mutation.model.MutationType;
import app.bpartners.geojobs.service.tiling.downloader.TilesDownloader;
import java.io.File;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MutationComputer {
  private final MutationApi api;
  private final TilesDownloader tilesDownloader;

  public MutationType apply(MutationContext context) {
    var mostRecentParcel = getMostRecentInstantParcel(context.parcelDelimitations());
    var oldParcel = getPrecedentInstantParcel(mostRecentParcel);

    var recentParcelImageFile = parcelImageFile(context, mostRecentParcel);
    var oldParcelImageFile = parcelImageFile(context, oldParcel);

    var filename = "mutation_" + UUID.randomUUID();
    var mutationResponse =
        api.detectMutation(
            oldParcelImageFile, recentParcelImageFile, context.maskImageFile(), filename);

    return mutationResponse.mutation();
  }

  // TODO: group the parcel delimitations by image date (millésime) and return the most recent one
  private InstantParcel getMostRecentInstantParcel(
      List<FeatureWithDelimitation> parcelDelimitations) {
    throw new NotImplementedException("Not implemented yet");
  }

  // TODO: return the instant parcel preceding the given one (previous millésime)
  private InstantParcel getPrecedentInstantParcel(InstantParcel parcel) {
    throw new NotImplementedException("Not implemented yet");
  }

  private File parcelImageFile(MutationContext context, InstantParcel parcel) {
    var parcelFeature = parcel.parcelDelimitations().getFirst().feature();
    var parcelContent =
        ParcelContent.builder()
            .id(parcelFeature.getId())
            .feature(parcelFeature)
            .geoServerUrl(context.geoServerUrl())
            .geoServerParameter(context.geoServerParameter())
            .creationDatetime(parcel.date())
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
