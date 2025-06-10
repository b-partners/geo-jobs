package app.bpartners.geojobs.service;

import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.MultiPolygon;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TileCoordinatesPolygonIntersection {
  private static final int DEFAULT_PIXEL_SIZE = 1024;
  private final GeometryPixelProjector geometryPixelProjector;
  private final GeometryConverter geometryConverter;

  public List<List<BigDecimal>> intersects(
      MultiPolygon latLonRoofMultiPolygon, TileCoordinates tileCoordinates) {
    var xTile = tileCoordinates.getX();
    var yTile = tileCoordinates.getY();
    var zTile = tileCoordinates.getZ();
    var multiPolygonFromTile = geometryConverter.getMultiPolygonFromTile(xTile, yTile, zTile);
    var multiPolygonGeoJsonMask = latLonRoofMultiPolygon.intersection(multiPolygonFromTile);
    return geometryPixelProjector.toPixels(
        multiPolygonGeoJsonMask, xTile, yTile, zTile, DEFAULT_PIXEL_SIZE);
  }
}
