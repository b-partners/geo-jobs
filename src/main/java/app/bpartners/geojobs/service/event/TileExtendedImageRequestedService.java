package app.bpartners.geojobs.service.event;

import static java.awt.Color.WHITE;

import app.bpartners.geojobs.endpoint.event.model.TileExtendedImageRequested;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.model.geometry.IntXY;
import app.bpartners.geojobs.service.FilePolygonDrawer;
import app.bpartners.geojobs.service.GeometryPixelProjector;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import app.bpartners.geojobs.service.tile19.ExtenderApi;
import app.bpartners.geojobs.service.tiling.TileFinder;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TileExtendedImageRequestedService implements Consumer<TileExtendedImageRequested> {
  private static final int DEFAULT_TILE_SIZE = 1024;
  private final TileFinder finder;
  private final BucketComponent bucketComponent;
  private final ExtenderApi extenderApi;
  private final FileWriter fileWriter;
  private final GeometryPixelProjector geometryPixelProjector;
  private final GeometryConverter geometryConverter;
  private final FilePolygonDrawer filePolygonDrawer;

  @Override
  public void accept(TileExtendedImageRequested event) {
    var layer = event.getLayer();
    var longitude = event.getLongitude();
    var latitude = event.getLatitude();
    var unifiedRoofMultiPolygon = event.getUnifiedRoofMultiPolygon();
    var tileCoordinates = finder.getSurroundingTiles(longitude, latitude, event.getZoom());
    var tileImagesFiles =
        tileCoordinates.stream()
            .map(
                coor -> {
                  var multiPolygonFromTile =
                      geometryConverter.getMultiPolygonFromTile(
                          coor.getX(), coor.getY(), coor.getZ());
                  var intersectionBetweenTileMultiPolygonAndRoofMultiPolygon =
                      unifiedRoofMultiPolygon.intersection(multiPolygonFromTile);
                  var notIntersectionBetweenTileMultiPolygonAndRoofMultiPolygon =
                      multiPolygonFromTile.difference(
                          intersectionBetweenTileMultiPolygonAndRoofMultiPolygon);
                  var fileKey =
                      layer + "/" + coor.getZ() + "/" + coor.getX() + "/" + coor.getY() + ".jpg";
                  List<IntXY> coordinatesPixel;
                  if (notIntersectionBetweenTileMultiPolygonAndRoofMultiPolygon.isEmpty()) {
                    // All images must be directly blured
                    coordinatesPixel =
                        List.of(
                            new IntXY(0, 0),
                            new IntXY(0, DEFAULT_TILE_SIZE),
                            new IntXY(DEFAULT_TILE_SIZE, DEFAULT_TILE_SIZE),
                            new IntXY(DEFAULT_TILE_SIZE, 0));
                  } else {
                    var backgroundPixels =
                        geometryPixelProjector.toPixels(
                            notIntersectionBetweenTileMultiPolygonAndRoofMultiPolygon,
                            coor.getX(),
                            coor.getY(),
                            coor.getZ(),
                            DEFAULT_TILE_SIZE);
                    coordinatesPixel =
                        backgroundPixels.stream()
                            .map(
                                coordinates ->
                                    new IntXY(
                                        coordinates.getFirst().intValue(),
                                        coordinates.getLast().intValue()))
                            .toList();
                  }
                  var originalImage = bucketComponent.download(fileKey);
                  return filePolygonDrawer.apply(coordinatesPixel, WHITE, originalImage);
                })
            .toList();

    var extendedImageBase64 = extenderApi.apply(tileImagesFiles);

    var filename = "extended_original_" + longitude + "_" + latitude;
    var extendedImageFile = fileWriter.base64ToFile(extendedImageBase64, filename);
    var extendedImageKey = layer + "/" + filename + ".jpg";
    bucketComponent.upload(extendedImageFile, extendedImageKey);

    extendedImageFile.delete();
  }
}
