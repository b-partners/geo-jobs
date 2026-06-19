package app.bpartners.geojobs.service.geojson;

import static app.bpartners.geojobs.endpoint.rest.postprocessing.BoundaryMerger.invert;
import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.service.geojson.GeoJson.fromFeatures;

import app.bpartners.geojobs.endpoint.rest.postprocessing.DetectionBoundaryMerger;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.LatLonPolygon;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TilingConf;
import app.bpartners.geojobs.model.ConversionFormatType;
import app.bpartners.geojobs.model.DetectedTile;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class GeoJsonConverter implements BiFunction<List<DetectedTile>, MultiPolygon, GeoJson> {
  private static final int DEFAULT_IMAGE_SIZE = 1024;
  private final GeoJsonMapper mapper;
  private final DetectionBoundaryMerger merger;
  private final GeometryConverter geometryConverter;

  @Override
  public GeoJson apply(
      List<DetectedTile> detectedTiles, MultiPolygon providedGeometryMultiPolygon) {
    List<GeoJson.GeoFeature> geoFeatures =
        detectedTiles.stream()
            .map(
                detectedTile -> {
                  var tile = detectedTile.getTile();
                  var tileCoordinates = tile.getCoordinates();
                  var xTile = tileCoordinates.getX();
                  var yTile = tileCoordinates.getY();
                  var zoom = tileCoordinates.getZ();
                  return mapper.toGeoFeatures(
                      xTile, yTile, zoom, DEFAULT_IMAGE_SIZE, detectedTile.getDetectedObjects());
                })
            .flatMap(List::stream)
            .toList();

    if (providedGeometryMultiPolygon != null) {
      geoFeatures.forEach(
          geoFeature -> {
            if (geoFeature.getGeometry() != null
                && geoFeature.getGeometry().getCoordinates() != null) {
              var multiPolygon = geometryConverter.apply(geoFeature.getGeometry().getCoordinates());
              var detectedGeoFeatureInsideProvidedGeometry =
                  providedGeometryMultiPolygon.intersection(multiPolygon);
              if (!detectedGeoFeatureInsideProvidedGeometry.isEmpty()) {
                if (detectedGeoFeatureInsideProvidedGeometry
                    instanceof MultiPolygon multiPolygonInsideProvidedGeometry) {
                  geoFeature.setGeometry(
                      geometryConverter.restMultiPolygonFromJts(
                          multiPolygonInsideProvidedGeometry));
                } else if (detectedGeoFeatureInsideProvidedGeometry
                    instanceof Polygon polygonInsideProvidedGeometry) {
                  var multiPolygonFromPolygon =
                      geometryFactory.createMultiPolygon(
                          new Polygon[] {polygonInsideProvidedGeometry});
                  geoFeature.setGeometry(
                      geometryConverter.restMultiPolygonFromJts(multiPolygonFromPolygon));
                } else {
                  log.error(
                      "Unable to handle geometry intersection type {} provided geometry with"
                          + " geoFeature.geometry : {}",
                      detectedGeoFeatureInsideProvidedGeometry.getClass().getSimpleName(),
                      geoFeature.getGeometry());
                }
              }
            }
          });
    }

    return fromFeatures(geoFeatures);
  }

  @NotNull
  private List<GeoJson.GeoFeature> unifyGeoFeatures(List<GeoJson.GeoFeature> geoFeatures) {
    var toUnify =
        geoFeatures.stream()
            .map(f -> LatLonPolygon.latLon(f).tiledPolygon(TilingConf.getDefaultInstance()))
            .collect(Collectors.toSet());

    var unifiedLatLon = merger.apply(toUnify, ConversionFormatType.GEO_JSON);
    var invertedUnifiedLatLon = invert(unifiedLatLon);
    var unifiedGeoFeatures =
        invertedUnifiedLatLon.stream().map(LatLonPolygon::toGeoFeature).toList();
    return unifiedGeoFeatures;
  }
}
