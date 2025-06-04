package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.model.Point.TypeEnum.POINT;

import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.endpoint.rest.model.Point;
import app.bpartners.geojobs.endpoint.rest.model.Polygon;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import java.util.List;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CentroidGeometryRetriever implements Function<Object, Point> {
  private final GeometryConverter geometryConverter;
  private final GeometryTiledValidator geometryTiledValidator;

  @Override
  public Point apply(Object geometry) {
    Point point;
    switch (geometry) {
      case Point p -> point = p;
      case Polygon providedPolygon -> {
        var geometryMultiPolygonProvided =
            geometryConverter.apply(List.of(providedPolygon.getCoordinates()));
        return retrieveFromProvidedValidMultiPolygon(geometryMultiPolygonProvided);
      }
      case MultiPolygon providedMultiPolygon -> {
        var geometryMultiPolygonProvided =
            geometryConverter.apply(providedMultiPolygon.getCoordinates());
        return retrieveFromProvidedValidMultiPolygon(geometryMultiPolygonProvided);
      }
      default -> throw new IllegalStateException("Unexpected value: " + geometry);
    }
    return point;
  }

  private Point retrieveFromProvidedValidMultiPolygon(
      org.locationtech.jts.geom.MultiPolygon geometryMultiPolygonProvided) {
    var centroidCoordinates = geometryConverter.centroidFromGeometry(geometryMultiPolygonProvided);
    var isMultiPolygonContainedInFrame = geometryTiledValidator.apply(geometryMultiPolygonProvided);
    if (isMultiPolygonContainedInFrame) {
      return new Point().coordinates(centroidCoordinates).type(POINT);
    }
    return null;
  }
}
