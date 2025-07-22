package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.repository.model.ArcgisImageZoom.HOUSES_0;

import app.bpartners.geojobs.service.geojson.GeometryConverter;
import app.bpartners.geojobs.service.tiling.TileFinder;
import java.util.List;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Polygon;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TilePolygonSplitter implements Function<Polygon, List<Polygon>> {
  private static final double TILE_3_X_3_AREA = 5_500.0;
  private final TileFinder tileFinder;
  private final GeometryConverter geometryConverter;
  private final GeometrySquareMeterArea geometrySquareMeterArea;

  @Override
  public List<Polygon> apply(Polygon geometryPolygon) {
    var actualGeometryArea = geometrySquareMeterArea.apply(geometryPolygon);
    if (actualGeometryArea > TILE_3_X_3_AREA) {
      var tileCoordinatesFromPolygon =
          tileFinder.getFromGeoJsonPolygon(geometryPolygon, HOUSES_0.getZoomLevel());
      return tileCoordinatesFromPolygon.stream()
          .map(
              coordinates ->
                  geometryConverter.getMultiPolygonFromTile(
                      coordinates.getX(), coordinates.getY(), coordinates.getZ()))
          .map(multiPolygon -> (org.locationtech.jts.geom.Polygon) multiPolygon.getGeometryN(0))
          .toList();
    }
    return List.of(geometryPolygon);
  }
}
