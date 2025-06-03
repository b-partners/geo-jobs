package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.model.Point.TypeEnum.POINT;

import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.endpoint.rest.model.Point;
import app.bpartners.geojobs.endpoint.rest.model.Polygon;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CentroidGeometryRetriever implements Function<Object, Point> {
  private final GeometryConverter geometryConverter;
  private final TileMultiPolygonFrame tileMultiPolygonFrame;

  @Override
  public Point apply(Object geometry) {
    Point point;
    switch (geometry) {
      case Point p -> point = p;
      case Polygon providedPolygon -> {
        var geometryMultiPolygonProvided =
            geometryConverter.apply(List.of(providedPolygon.getCoordinates()));
        point =
            retrieveFromProvidedGeoJsonCentroidPoint(
                geometryConverter.centroidFromGeometry(providedPolygon),
                geometryMultiPolygonProvided);
      }
      case MultiPolygon providedMultiPolygon -> {
        var geometryMultiPolygonProvided =
            geometryConverter.apply(providedMultiPolygon.getCoordinates());
        point =
            retrieveFromProvidedGeoJsonCentroidPoint(
                geometryConverter.centroidFromGeometry(providedMultiPolygon),
                geometryMultiPolygonProvided);
      }
      default -> throw new IllegalStateException("Unexpected value: " + geometry);
    }
    return point;
  }

  private Point retrieveFromProvidedGeoJsonCentroidPoint(
      List<BigDecimal> centroidCoordinates,
      org.locationtech.jts.geom.MultiPolygon geometryMultiPolygonProvided) {
    var centroidPoint = new Point().coordinates(centroidCoordinates).type(POINT);

    var longitude = centroidPoint.getCoordinates().getFirst();
    var latitude = centroidPoint.getCoordinates().getLast();
    var optionalMultiPolygonTiles = tileMultiPolygonFrame.apply(longitude, latitude);
    if (optionalMultiPolygonTiles.isPresent()
        && optionalMultiPolygonTiles.get().contains(geometryMultiPolygonProvided)) {
      return centroidPoint;
    }
    return null;
  }
}
