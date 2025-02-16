package app.bpartners.geojobs.endpoint.rest.postprocessing.model;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static java.lang.Math.PI;
import static java.lang.Math.atan;
import static java.lang.Math.pow;
import static java.lang.Math.sinh;
import static java.lang.Math.toDegrees;

import app.bpartners.geojobs.endpoint.rest.model.DetectedObject;
import app.bpartners.geojobs.endpoint.rest.model.DetectedTile;
import app.bpartners.geojobs.model.geometry.IntXY;
import app.bpartners.geojobs.model.geometry.TileCoordinatesFromFileName;
import app.bpartners.geojobs.model.geometry.VGG;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

@Slf4j
// projected: in meter, such as CRS_CODE = "EPSG:3857"
public record TiledPolygon(Polygon polygon, IntXY originTile, TilingConf tilingConf) {

  public LatLonPolygon latLonPolygon() {
    var latLonCoordinates =
        Arrays.stream(polygon.getCoordinates())
            .map(c -> toLatLon(originTile, tilingConf, new IntXY((int) c.x, (int) c.y)))
            .toArray(Coordinate[]::new);
    var currentLength = latLonCoordinates.length;
    var firstLatLon = latLonCoordinates[0];
    var copyOfLatLonCoordinates = Arrays.copyOf(latLonCoordinates, currentLength + 1);
    copyOfLatLonCoordinates[currentLength] = firstLatLon;
    return new LatLonPolygon(geometryFactory.createPolygon(copyOfLatLonCoordinates));
  }

  public static Set<TiledPolygon> newTiledPolygons(Set<DetectedTile> tiles, int imgSize) {
    Set<TiledPolygon> res = new HashSet<>();
    for (var t : tiles) {
      for (var o : t.getDetectedObjects()) {
        var tiledPolygon = tiledPolygon(t, o, imgSize);
        res.add(tiledPolygon);
      }
    }
    return res;
  }

  public static Set<TiledPolygon> newTiledPolygons(
      String filename,
      Map<String, VGG.Annotation.Region> vggRegions,
      TilingConf tilingConf,
      boolean isZXYDotFiletype) {
    Set<TiledPolygon> res = new HashSet<>();
    var regions = vggRegions.values();
    for (var r : regions) {
      res.add(tiledPolygon(filename, r, tilingConf, isZXYDotFiletype));
    }
    return res;
  }

  private static TiledPolygon tiledPolygon(
      DetectedTile detectedTile, DetectedObject detectedObject, int imgSize) {
    var restPolygon = detectedObject.getFeature().getGeometry().getPolygon();
    var tileInfo = detectedTile.getTileInfo();
    var tileCoordinates = tileInfo.getCoordinates();
    var originTile = new IntXY(tileCoordinates.getX(), tileCoordinates.getZ());
    return new TiledPolygon(
        polygon(restPolygon), originTile, new TilingConf(tileCoordinates.getZ(), imgSize));
  }

  private static TiledPolygon tiledPolygon(
      String filename,
      VGG.Annotation.Region vggRegion,
      TilingConf tilingConf,
      boolean isZXYDotFiletype) {
    var shapeAttribute = vggRegion.getShapeAttribute();
    var coordsExtractor = new TileCoordinatesFromFileName(isZXYDotFiletype);
    var originTile = new IntXY(coordsExtractor.x(filename), coordsExtractor.y(filename));
    return new TiledPolygon(polygon(shapeAttribute), originTile, tilingConf);
  }

  private static Polygon polygon(app.bpartners.geojobs.endpoint.rest.model.Polygon restP) {
    List<List<List<BigDecimal>>> restPCoordinates = restP.getCoordinates();
    if (restPCoordinates.size() != 1) {
      throw new IllegalArgumentException("Single Polygon expected but got: " + restP);
    }

    var onlyPolygonCoordinates = restPCoordinates.get(0);
    return geometryFactory.createPolygon(
        onlyPolygonCoordinates.stream()
            .map(c -> new Coordinate(c.get(0).doubleValue(), c.get(1).doubleValue()))
            .toArray(Coordinate[]::new));
  }

  private static Polygon polygon(VGG.Annotation.Region.ShapeAttribute vggShapeAttribute) {
    var allX = vggShapeAttribute.getAllPointsX();
    var allY = vggShapeAttribute.getAllPointsY();
    var polygonLength = allX.size();
    Coordinate[] coordinates = new Coordinate[polygonLength];
    for (int i = 0; i < polygonLength; i++) {
      var pixel = new IntXY(allX.get(i).intValue(), allY.get(i).intValue());
      coordinates[i] = new Coordinate(pixel.x(), pixel.y());
    }
    return geometryFactory.createPolygon(coordinates);
  }

  // Mostly ChatGPT-generated
  public static Coordinate toLatLon(IntXY originTile, TilingConf tilingConf, IntXY pixel) {
    int tileSize = tilingConf.imgSize();
    int scale = tilingConf.imgSize() / tileSize; // Scale factor (4x)

    // Convert image pixel to tile pixel
    double tilePX = pixel.x() / (double) scale;
    double tilePY = pixel.y() / (double) scale;

    // Convert back to lat/lon
    double n = pow(2, tilingConf.z());
    double lon = (originTile.x() + tilePX / tileSize) / n * 360.0 - 180.0;
    double lat = toDegrees(atan(sinh(PI * (1 - 2 * (originTile.y() + tilePY / tileSize) / n))));

    return new Coordinate(lat, lon);
  }
}
