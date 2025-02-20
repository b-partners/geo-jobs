package app.bpartners.geojobs.endpoint.rest.postprocessing.continuer;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static java.util.stream.Collectors.toSet;

import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TiledPolygon;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TilingConf;
import app.bpartners.geojobs.model.geometry.IntXY;
import app.bpartners.geojobs.model.geometry.route.Route;
import app.bpartners.geojobs.model.geometry.route.RouteType;
import app.bpartners.geojobs.model.geometry.route.RoutesContinuation;
import app.bpartners.geojobs.model.geometry.route.RoutesContinuationConf;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

@AllArgsConstructor
public final class TiledLinesContinuer extends LinesContinuer<TiledPolygon> {

  private final RoutesContinuationConf routesContinuationConf;

  @Accessors(fluent = true)
  @Getter
  private final TilingConf tilingConf;

  @Override
  public Set<TiledPolygon> apply(Set<TiledPolygon> polygons) {
    var originTile = new ArrayList<>(polygons).getFirst().originTile();
    var routessWithOffset =
        polygons.stream()
            .map(
                p ->
                    new Route(
                        withOffset(p, originTile, p.originTile()),
                        RouteType.road /*TODO(routeType)*/))
            .collect(toSet());
    var continuedWithOffset =
        new RoutesContinuation(routessWithOffset, routesContinuationConf).continued();
    return continuedWithOffset.stream()
        .map(
            pWithOffset ->
                new TiledPolygon(
                    pWithOffset, (RouteType) pWithOffset.getUserData(), originTile, tilingConf))
        .collect(toSet());
  }

  private Polygon withOffset(TiledPolygon p, IntXY originTile, IntXY currentTile) {
    var imgSize = tilingConf.imgSize();
    var xFactor = currentTile.x() - originTile.x();
    var yFactor = currentTile.y() - originTile.y();
    var polygon =
        geometryFactory.createPolygon(
            Arrays.stream(p.polygon().getCoordinates())
                .map(c -> new Coordinate(c.x + xFactor * imgSize, c.y + yFactor * imgSize))
                .toArray(Coordinate[]::new));
    polygon.setUserData(p.type());
    return polygon;
  }
}
