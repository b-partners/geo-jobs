package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.service.geojson.GeometryConverter.unifyMultiPolygon;

import app.bpartners.geojobs.endpoint.rest.model.Point;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import java.util.List;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.MultiPolygon;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DetectionRoofProvider implements Function<Detection, MultiPolygon> {
  private final GeometryConverter geometryConverter;

  @Override
  public MultiPolygon apply(Detection detection) {
    return detection.getProvidedGeoJsonZone().stream()
        .map(
            feature -> {
              var geometryType = feature.getGeometry().getActualInstance();
              MultiPolygon multiPolygon;
              switch (geometryType) {
                case Point point ->
                    multiPolygon = geometryConverter.retrieveNearestRoofMultiPolygon(point);
                case app.bpartners.geojobs.endpoint.rest.model.Polygon restPolygon ->
                    multiPolygon = geometryConverter.apply(List.of(restPolygon.getCoordinates()));
                case app.bpartners.geojobs.endpoint.rest.model.MultiPolygon restMultiPolygon ->
                    multiPolygon = geometryConverter.apply(restMultiPolygon.getCoordinates());
                default ->
                    throw new IllegalStateException("Unexpected geometry type: " + geometryType);
              }
              return multiPolygon;
            })
        .reduce(unifyMultiPolygon())
        .orElseThrow(() -> new NotFoundException("No roof polygon found for provided zone"));
  }
}
