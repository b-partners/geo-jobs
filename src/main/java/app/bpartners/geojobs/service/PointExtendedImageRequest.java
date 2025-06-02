package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.model.Point.TypeEnum.POINT;
import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.repository.model.ArcgisImageZoom.HOUSES_0;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.TileExtendedImageRequested;
import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import app.bpartners.geojobs.service.tiling.TileFinder;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PointExtendedImageRequest implements BiFunction<Feature, String, Feature> {
  private final EventProducer eventProducer;
  private final GeometryConverter geometryConverter;
  private final TileFinder tileFinder;

  @Override
  public Feature apply(Feature feature, String layer) {
    var geometry = Objects.requireNonNull(feature.getGeometry()).getActualInstance();
    Point point;
    switch (geometry) {
      case Point p -> point = p;
      case Polygon providedPolygon -> {
        var geometryMultiPolygonProvided =
            geometryConverter.apply(List.of(providedPolygon.getCoordinates()));
        point =
            retrieveFromProvidedGeoJsonCentroidPoint(
                geometryConverter.centroidFromMultiPolygon(providedPolygon),
                geometryMultiPolygonProvided);
      }

      case MultiPolygon providedMultiPolygon -> {
        var geometryMultiPolygonProvided =
            geometryConverter.apply(providedMultiPolygon.getCoordinates());
        point =
            retrieveFromProvidedGeoJsonCentroidPoint(
                geometryConverter.centroidFromMultiPolygon(providedMultiPolygon),
                geometryMultiPolygonProvided);
      }
      default -> throw new IllegalStateException("Unexpected value: " + geometry);
    }
    if (point != null) {
      var pointCoordinates = point.getCoordinates();
      var longitude = pointCoordinates.getFirst();
      var latitude = pointCoordinates.getLast();
      var defaultZoomLevel = HOUSES_0.getZoomLevel();

      eventProducer.accept(
          List.of(new TileExtendedImageRequested(longitude, latitude, defaultZoomLevel, layer)));
    }
    return feature;
  }

  private Point retrieveFromProvidedGeoJsonCentroidPoint(
      List<BigDecimal> centroidCoordinates,
      org.locationtech.jts.geom.MultiPolygon geometryMultiPolygonProvided) {
    var centroidPoint = new Point().coordinates(centroidCoordinates).type(POINT);

    var longitude = centroidPoint.getCoordinates().getFirst();
    var latitude = centroidPoint.getCoordinates().getLast();
    var tileCoordinates =
        tileFinder.getSurroundingTiles(longitude, latitude, HOUSES_0.getZoomLevel());
    var optionalMultiPolygonTiles =
        tileCoordinates.stream()
            .map(
                tileCoordinate ->
                    geometryConverter.getMultiPolygonFromTile(
                        tileCoordinate.getX(), tileCoordinate.getY(), tileCoordinate.getZ()))
            .reduce(
                (multiPolygon1, multiPolygon2) -> {
                  var unifiedGeometry = multiPolygon1.union(multiPolygon2);
                  if (unifiedGeometry
                      instanceof org.locationtech.jts.geom.MultiPolygon multiPolygon) {
                    return multiPolygon;
                  } else if (unifiedGeometry instanceof org.locationtech.jts.geom.Polygon polygon) {
                    return geometryFactory.createMultiPolygon(
                        new org.locationtech.jts.geom.Polygon[] {polygon});
                  }
                  throw new UnsupportedOperationException(
                      "Unsupported unified geometry : " + unifiedGeometry);
                });
    if (optionalMultiPolygonTiles.isPresent()
        && optionalMultiPolygonTiles.get().contains(geometryMultiPolygonProvided)) {
      return centroidPoint;
    }
    return null;
  }
}
