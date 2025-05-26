package app.bpartners.geojobs.endpoint.rest.postprocessing.model;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.model.geometry.route.RouteType.routeTypeFrom;
import static java.lang.Math.PI;
import static java.lang.Math.cos;
import static java.lang.Math.floor;
import static java.lang.Math.log;
import static java.lang.Math.pow;
import static java.lang.Math.round;
import static java.lang.Math.tan;
import static java.lang.Math.toRadians;
import static java.util.Comparator.comparingInt;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.*;

import app.bpartners.geojobs.model.geometry.IntXY;
import java.util.Arrays;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.geotools.geometry.Position2D;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

// unprojected: in degree, such as CRS_CODE = "EPSG:4326"
@Slf4j
public record LatLonPolygon(Polygon polygon) {
  private static final String DEFAULT_LABEL = "line";

  public TiledPolygon tiledPolygon(TilingConf tilingConf) {
    Map<String, String> userData = (Map) polygon.getUserData();
    var label = userData == null ? DEFAULT_LABEL : userData.get("label");
    var originXY = uniqueOrigin(tilingConf);
    return new TiledPolygon(
        toPixelPolygon(polygon, tilingConf, originXY), routeTypeFrom(label), originXY, tilingConf);
  }

  private IntXY uniqueOrigin(TilingConf tilingConf) {
    var origins =
        Arrays.stream(polygon.getCoordinates())
            .map(m -> originTile(m, tilingConf.z()))
            .map(tileXY -> new Position2D(tileXY.x(), tileXY.y()))
            .collect(groupingBy(identity(), collectingAndThen(counting(), Long::intValue)));
    if (origins.size() != 1) {
      log.warn("origins.size=1 expected: origins={}, p={}", origins, polygon);
    }
    var origin =
        origins.entrySet().stream()
            .max(comparingInt(Map.Entry::getValue))
            .orElseThrow(() -> new IllegalStateException("No origin found"))
            .getKey();

    return new IntXY((int) origin.x, (int) origin.y);
  }

  // Mostly ChatGPT-generated
  public static IntXY originTile(Coordinate mercatorCoordinate, int z) {
    double n = pow(2, z);
    int tileX = (int) floor((mercatorCoordinate.y + 180) / 360 * n);
    double mercXInRad = toRadians(mercatorCoordinate.x);
    double tileYToFloor = (1 - log(tan(mercXInRad) + 1 / cos(mercXInRad)) / PI) / 2 * n;
    int tileY = (int) floor(tileYToFloor);
    return new IntXY(tileX, tileY);
  }

  private Polygon toPixelPolygon(Polygon p, TilingConf tilingConf, IntXY originTile) {
    var latLonCoordinates = p.getExteriorRing().getCoordinates();
    var pixelCoordinates =
        Arrays.stream(latLonCoordinates)
            .map(c -> toPixel(new LatLon(c.x, c.y), tilingConf, originTile))
            .map(pixel -> new Coordinate(pixel.x(), pixel.y()))
            .toArray(Coordinate[]::new);
    return geometryFactory.createPolygon(pixelCoordinates);
  }

  // Mostly ChatGPT-generated
  public static IntXY toPixel(LatLon latLon, TilingConf tilingConf) {
    return toPixel(
        latLon, tilingConf, originTile(new Coordinate(latLon.lat(), latLon.lon()), tilingConf.z()));
  }

  public static IntXY toPixel(LatLon latLon, TilingConf tilingConf, IntXY originTile) {
    int tileSize = 256; // Default tile size
    int scale = tilingConf.imgSize() / tileSize; // Scale factor (4x)
    int z = tilingConf.z();
    double n = pow(2, z);

    // Compute pixel within tile
    var lat = latLon.lat();
    var lon = latLon.lon();
    double x = ((lon + 180) / 360 * n - originTile.x()) * tileSize;
    double y =
        ((1 - log(tan(toRadians(lat)) + 1 / cos(toRadians(lat))) / PI) / 2 * n - originTile.y())
            * tileSize;

    // Convert to 1024x1024 image pixels
    int imgX = (int) round(x * scale);
    int imgY = (int) round(y * scale);

    return new IntXY(imgX, imgY);
  }
}
