package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.TileExtendedImageRequested;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.service.tile19.ExtenderApi;
import app.bpartners.geojobs.service.tiling.TileFinder;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TileExtendedImageRequestedService implements Consumer<TileExtendedImageRequested> {
  private final TileFinder finder;
  private final BucketComponent bucketComponent;
  private final ExtenderApi extenderApi;
  private final FileWriter fileWriter;

  @Override
  public void accept(TileExtendedImageRequested event) {
    var layer = event.getLayer();
    var longitude = event.getLongitude();
    var latitude = event.getLatitude();
    var tileCoordinates = finder.getSurroundingTiles(longitude, latitude, event.getZoom());
    var tileImagesFiles =
        tileCoordinates.stream()
            .map(coor -> layer + "/" + coor.getZ() + "/" + coor.getX() + "/" + coor.getY() + ".jpg")
            .map(bucketComponent::download)
            .toList();

    var extendedImageBase64 = extenderApi.apply(tileImagesFiles);

    var filename = "extended_" + longitude + "_" + latitude;
    var extendedImageFile = fileWriter.base64ToFile(extendedImageBase64, filename);
    var extendedImageKey = layer + "/" + filename + ".jpg";
    bucketComponent.upload(extendedImageFile, extendedImageKey);

    extendedImageFile.delete();
  }
}
