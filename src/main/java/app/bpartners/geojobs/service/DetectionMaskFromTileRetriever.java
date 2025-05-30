package app.bpartners.geojobs.service;

import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.detection.DetectionMaskCreator;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import java.io.File;
import java.math.BigDecimal;
import java.util.List;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.MultiPolygon;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DetectionMaskFromTileRetriever implements BiFunction<Tile, MultiPolygon, File> {
  private static final int DEFAULT_PIXEL_SIZE = 1024;
  private final GeometryPixelProjector geometryPixelProjector;
  private final GeometryConverter geometryConverter;
  private final DetectionMaskCreator maskCreator;

  @Override
  public File apply(Tile tile, MultiPolygon roofMultiPolygon) {
    var tileCoordinates = tile.getCoordinates();
    var xTile = tileCoordinates.getX();
    var yTile = tileCoordinates.getY();
    var zTile = tileCoordinates.getZ();
    var multiPolygonFromTile = geometryConverter.getMultiPolygonFromTile(xTile, yTile, zTile);
    var multiPolygonGeoJsonMask = roofMultiPolygon.intersection(multiPolygonFromTile);
    List<List<BigDecimal>> projectorPixels =
        geometryPixelProjector.toPixels(
            multiPolygonGeoJsonMask, xTile, yTile, zTile, DEFAULT_PIXEL_SIZE);
    return maskCreator.apply(projectorPixels);
  }
}
