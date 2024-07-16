package app.bpartners.geojobs.service.geojson;

import org.locationtech.jts.geom.Coordinate;

import java.math.BigDecimal;
import java.util.List;

public class GeoReferencer {
  // distances are estimated in kilometer
  private static final double EARTH_CIRCUMFERENCE = 40075016.686;
  private static final double KM_PER_DEG_LAT = 110.574;
  private static final double KM_PER_DEG_LON = 111.320;

  public static List<BigDecimal> toGeographicalCoordinates(
      int xTile, int yTile, double x, double y, int zoom, int imageWidth) {
    var latitudeRef = tile2deg(xTile, yTile, zoom).y;
    var longitudeRef = tile2deg(xTile, yTile, zoom).x;
    var latitudeInRadians = latitudeRef * (Math.PI / 180);
    // https://wiki.openstreetmap.org/wiki/Zoom_levels
    var scale =
        EARTH_CIRCUMFERENCE * Math.cos(latitudeInRadians) / (Math.pow(2, zoom) * imageWidth);
    // from meter to kilometer
    var dy = (y * scale) / 1000;
    var dx = (x * scale) / 1000;
    var targetLatitude = BigDecimal.valueOf(latitudeRef + dy / KM_PER_DEG_LAT);
    var targetLongitude = BigDecimal.valueOf(longitudeRef + (dx / (KM_PER_DEG_LON * Math.cos(latitudeInRadians))));
    return List.of(targetLongitude, targetLatitude);
  }

  private static Coordinate tile2deg(int xTile, int yTile, int zoom) {
    var bias = Math.pow(2, zoom);
    var longitude = xTile / bias * 360.0 - 180.0;
    var latitudeInRadians = Math.atan(Math.sinh((1 - 2 * yTile / bias) * Math.PI));
    var latitude = Math.toDegrees(latitudeInRadians);
    return new Coordinate(longitude, latitude);
  }
}
