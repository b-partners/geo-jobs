package app.bpartners.geojobs.service.geojson;

import static app.bpartners.geojobs.endpoint.rest.model.MultiPolygon.TypeEnum.MULTI_POLYGON;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.TOITURE_REVETEMENT;
import static app.bpartners.geojobs.service.geojson.GeoReferencer.toGeographicalCoordinates;

import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.repository.model.detection.DetectedObject;
import java.math.BigDecimal;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeoJsonMapper {
  private final GeoJsonMultiPolygonCorrector geoJsonMultiPolygonCorrector;

  public List<GeoJson.GeoFeature> toGeoFeatures(
      int xTile, int yTile, int zoom, int imageWidth, List<DetectedObject> detectedObjects) {
    var geoFeatures = new ArrayList<GeoJson.GeoFeature>();
    detectedObjects.stream()
        .filter(
            detectedObject ->
                detectedObject.getFeature() != null
                    && detectedObject.getFeature().getGeometry() != null
                    && !TOITURE_REVETEMENT.equals(
                        detectedObject.getDetectedObjectType().getDetectableType()))
        .forEach(
            object -> {
              var feature = object.getFeature();
              var geometry = feature.getGeometry();
              log.info("detected object geometry: {}", geometry);
              var actualGeometryInstance = geometry.getActualInstance();
              if (actualGeometryInstance.getClass().equals(MultiPolygon.class)) {
                var multiPolygon = (MultiPolygon) actualGeometryInstance;
                if (multiPolygon.getCoordinates() == null) {
                  throw new IllegalArgumentException("Multipolygon coordinates should not be null");
                }
                var fixedMultiPolygon = geoJsonMultiPolygonCorrector.apply(multiPolygon);
                geoFeatures.add(
                    mapToFeature(
                        xTile,
                        yTile,
                        zoom,
                        imageWidth,
                        object,
                        Objects.requireNonNull(fixedMultiPolygon.getCoordinates())));
              } else {
                throw new NotImplementedException(
                    "Only MultiPolygon geometry is supported for now but actual geometry class : "
                        + geometry.getActualInstance().getClass()
                        + " for detectedObject(id="
                        + object.getId()
                        + ", type="
                        + object.getDetectedObjectType().getDetectableType());
              }
            });
    return geoFeatures;
  }

  private GeoJson.GeoFeature mapToFeature(
      int xTile,
      int yTile,
      int zoom,
      int imageWidth,
      DetectedObject object,
      List<List<List<List<BigDecimal>>>> geometryCoordinates) {
    List<List<List<List<BigDecimal>>>> multipolygonCoordinates =
        getMultipolygonCoordinates(xTile, yTile, zoom, imageWidth, geometryCoordinates);
    var objectFeature = object.getFeature();
    var properties =
        objectFeature.getProperties() == null
            ? new HashMap<String, Object>()
            : objectFeature.getProperties();
    var confidence =
        object.getComputedConfidence() != null ? object.getComputedConfidence().toString() : null;
    properties.put("confidence", confidence);
    properties.put("label", object.getDetectedObjectType().getDetectableType().name());
    return getGeoFeature(multipolygonCoordinates, properties);
  }

  public GeoJson.GeoFeature getGeoFeature(
      List<List<List<List<BigDecimal>>>> multipolygonCoordinates, Map<String, Object> properties) {
    var multipolygon = new MultiPolygon();
    multipolygon.setType(MULTI_POLYGON);
    multipolygon.setCoordinates(multipolygonCoordinates);
    return new GeoJson.GeoFeature(properties, multipolygon);
  }

  private List<List<List<List<BigDecimal>>>> getMultipolygonCoordinates(
      int xTile,
      int yTile,
      int zoom,
      int imageWidth,
      List<List<List<List<BigDecimal>>>> geometryCoordinates) {
    return geometryCoordinates.stream()
        .map(
            xyzMultiPolygon ->
                xyzMultiPolygon.stream()
                    .map(
                        xyzPolygon -> {
                          List<List<BigDecimal>> geoPolygon =
                              xyzPolygon.stream()
                                  .map(
                                      points -> {
                                        if (points.isEmpty()) {
                                          return new ArrayList<BigDecimal>();
                                        }
                                        var x = points.getFirst();
                                        var y = points.getLast();
                                        if (xTile == 0 && yTile == 0) {
                                          return List.of(x, y);
                                        }
                                        return toGeographicalCoordinates(
                                            xTile,
                                            yTile,
                                            x.doubleValue(),
                                            y.doubleValue(),
                                            zoom,
                                            imageWidth);
                                      })
                                  .toList();
                          if (geoPolygon.isEmpty()) {
                            return geoPolygon;
                          }
                          List<BigDecimal> first = geoPolygon.getFirst();
                          List<BigDecimal> last = geoPolygon.getLast();
                          if (!first.equals(last)) {
                            List<List<BigDecimal>> closedGeoPolygon = new ArrayList<>(geoPolygon);
                            closedGeoPolygon.add(new ArrayList<>(first));
                            return closedGeoPolygon;
                          }
                          return geoPolygon;
                        })
                    .toList())
        .toList();
  }
}
