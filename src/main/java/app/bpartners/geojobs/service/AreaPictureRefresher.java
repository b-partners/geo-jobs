package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.postprocessing.model.LatLonPolygon.originTile;

import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.endpoint.rest.model.TileInfo;
import app.bpartners.geojobs.endpoint.rest.model.TileInfoSize;
import app.bpartners.geojobs.repository.model.ArcgisImageZoom;
import app.bpartners.geojobs.repository.model.AreaPicture;
import app.bpartners.geojobs.service.tiling.downloader.GeoCodeApi;
import lombok.AllArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AreaPictureRefresher {
  private static final int DEFAULT_IMG_SIZE = 1024;
  private final GeoCodeApi geoCodeApi;

  public AreaPicture refreshTile(AreaPicture areaPicture) {
    var geoPosition = geoCodeApi.apply(areaPicture.getAddress());
    var mercatorCoords = new Coordinate(geoPosition.getLatitude(), geoPosition.getLongitude());

    var zoom = ArcgisImageZoom.from(areaPicture.getZoom().getLevel());
    var originTile = originTile(mercatorCoords, zoom);

    var currentTile =
        new TileInfo()
            .size(new TileInfoSize().height(DEFAULT_IMG_SIZE).width(DEFAULT_IMG_SIZE))
            .coordinates(new TileCoordinates().x(originTile.x()).y(originTile.y()).z(zoom));
    areaPicture.setCurrentTile(currentTile);

    return areaPicture;
  }
}
