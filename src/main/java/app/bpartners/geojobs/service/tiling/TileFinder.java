package app.bpartners.geojobs.service.tiling;

import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TileFinder {

  private int lonToTileX(double lon, int zoom) {
    return (int) Math.floor((lon + 180) / 360 * Math.pow(2, zoom));
  }

  private int latToTileY(double lat, int zoom) {
    return (int)
        Math.floor(
            (1
                    - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat)))
                        / Math.PI)
                / 2
                * Math.pow(2, zoom));
  }

  public List<TileCoordinates> getSurroundingTiles(BigDecimal lon, BigDecimal lat, int zoom) {
    int x = lonToTileX(lon.doubleValue(), zoom);
    int y = latToTileY(lat.doubleValue(), zoom);

    List<TileCoordinates> tileCoordinates = new ArrayList<>();
    for (int dx = -1; dx <= 1; dx++) {
      for (int dy = -1; dy <= 1; dy++) {
        tileCoordinates.add(new TileCoordinates().x(x + dx).y(y + dy).z(zoom));
      }
    }
    tileCoordinates.sort(
        Comparator.comparing(TileCoordinates::getZ)
            .thenComparing(TileCoordinates::getY)
            .thenComparing(TileCoordinates::getX));
    return tileCoordinates;
  }
}
