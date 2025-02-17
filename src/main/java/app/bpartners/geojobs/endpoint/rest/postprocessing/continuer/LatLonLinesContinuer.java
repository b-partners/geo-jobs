package app.bpartners.geojobs.endpoint.rest.postprocessing.continuer;

import static java.util.stream.Collectors.toSet;

import app.bpartners.geojobs.endpoint.rest.postprocessing.model.LatLonPolygon;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TiledPolygon;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TilingConf;
import app.bpartners.geojobs.model.geometry.route.RoutesContinuationConf;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.geojson.feature.FeatureJSON;
import org.locationtech.jts.geom.Polygon;

public final class LatLonLinesContinuer extends LinesContinuer<LatLonPolygon> {

  private final TiledLinesContinuer tiledLinesContinuer;

  public LatLonLinesContinuer(
      RoutesContinuationConf routesContinuationConf, TilingConf tilingConf) {
    this.tiledLinesContinuer = new TiledLinesContinuer(routesContinuationConf, tilingConf);
  }

  @Override
  public Set<LatLonPolygon> apply(Set<LatLonPolygon> latLonPolygons) {
    var tilingConf = tiledLinesContinuer.tilingConf();
    var tiledPolygons =
        latLonPolygons.stream().map(p -> p.tiledPolygon(tilingConf)).collect(toSet());
    var continuedTiledPolygons = tiledLinesContinuer.apply(tiledPolygons);
    return continuedTiledPolygons.stream().map(TiledPolygon::latLonPolygon).collect(toSet());
  }

  public Set<LatLonPolygon> apply(File geojsonPath) {
    Set<LatLonPolygon> latLonPolygons = new HashSet<>();

    var featureJson = new FeatureJSON();
    try (FileReader reader = new FileReader(geojsonPath)) {
      var featureCollection = featureJson.readFeatureCollection(reader);
      try (var featuresIterator = featureCollection.features()) {
        while (featuresIterator.hasNext()) {
          SimpleFeature feature = (SimpleFeature) featuresIterator.next();
          latLonPolygons.add(new LatLonPolygon((Polygon) feature.getDefaultGeometry()));
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return apply(latLonPolygons);
  }
}
