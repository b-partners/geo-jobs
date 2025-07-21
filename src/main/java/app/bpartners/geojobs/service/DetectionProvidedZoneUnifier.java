package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.service.geojson.GeometryConverter.unifyMultiPolygon;

import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import java.util.List;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DetectionProvidedZoneUnifier implements Function<Detection, MultiPolygon> {
  private final GeometryConverter geometryConverter;

  @Override
  public MultiPolygon apply(Detection detection) {
    if (detection == null) {
      return geometryFactory.createMultiPolygon(new Polygon[0]);
    }
    var providedGeoJsonZone = detection.getProvidedGeoJsonZone();
    return apply(detection.getId(), providedGeoJsonZone);
  }

  public MultiPolygon applyMultiGeoJson(Detection detection) {
    return apply(detection.getId(), detection.getMultiPolygonGeoJsonZone());
  }

  private MultiPolygon apply(String detectionId, List<Feature> featureList) {
    if (featureList == null) {
      return geometryFactory.createMultiPolygon(new Polygon[0]);
    }
    if (featureList.isEmpty()) {
      return geometryFactory.createMultiPolygon(new Polygon[0]);
    }
    return featureList.stream()
        .map(
            feature -> {
              var geometryType = feature.getGeometry().getActualInstance();
              MultiPolygon multiPolygonJts;
              switch (geometryType) {
                case app.bpartners.geojobs.endpoint.rest.model.Polygon polygon ->
                    multiPolygonJts = geometryConverter.apply(List.of(polygon.getCoordinates()));
                case app.bpartners.geojobs.endpoint.rest.model.MultiPolygon multiPolygon ->
                    multiPolygonJts = geometryConverter.apply(multiPolygon.getCoordinates());
                default ->
                    throw new UnsupportedOperationException(
                        "Unsupported geometry type to retrieve multiPolygon for"
                            + " tileDetectionTask : "
                            + geometryType);
              }
              return multiPolygonJts;
            })
        .reduce(unifyMultiPolygon())
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "Unable to unify provided zone for detection.id : " + detectionId));
  }
}
