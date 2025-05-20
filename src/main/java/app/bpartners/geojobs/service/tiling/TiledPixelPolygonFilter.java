package app.bpartners.geojobs.service.tiling;

import app.bpartners.geojobs.model.geometry.TiledPixelPolygon;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TiledPixelPolygonFilter {
  private final TileProjection tileProjection;

  public List<TiledPixelPolygon> filterPolygonsInMask(
      List<TiledPixelPolygon> pixelPolygons, Geometry maskGeoJson) {
    var preparedMask = new PreparedGeometryFactory().create(maskGeoJson);
    return pixelPolygons.stream()
        .filter(
            tiledPixelPolygon -> {
              var geoPolygons =
                  tiledPixelPolygon.polygons().stream()
                      .map(
                          pixelPolygon ->
                              tileProjection.pixelPolygonToGeo(
                                  pixelPolygon.polygon(),
                                  tiledPixelPolygon.tileX(),
                                  tiledPixelPolygon.tileY(),
                                  tiledPixelPolygon.zoom()))
                      .toList();
              return geoPolygons.stream().anyMatch(preparedMask::intersects);
            })
        .toList();
  }
}
