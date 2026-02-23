package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.model.DelimitationObjectType.BUILDING;
import static app.bpartners.geojobs.repository.model.ArcgisImageZoom.HOUSES_0;

import app.bpartners.geojobs.endpoint.rest.model.Point;
import app.bpartners.geojobs.model.DelimitationObjectType;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FeaturePointConverter implements BiFunction<Point, DelimitationObjectType, Feature> {
  private final GeometryConverter geometryConverter;

  @Override
  public Feature apply(Point point, DelimitationObjectType delimitationObjectType) {
    if (BUILDING.equals(delimitationObjectType)) {
      var longitude = point.getCoordinates().getFirst();
      var latitude = point.getCoordinates().getLast();
      var nearestRoofMultiPolygon =
          geometryConverter.retrieveNearestRoofMultiPolygon(List.of(longitude, latitude));
      var properties = new HashMap<String, Object>();
      return geometryConverter.toFeature(
          null, HOUSES_0.getZoomLevel(), properties, nearestRoofMultiPolygon);
    }
    throw new NotImplementedException(
        "Unable to convert address to Feature for delimitationObjectType "
            + delimitationObjectType);
  }
}
