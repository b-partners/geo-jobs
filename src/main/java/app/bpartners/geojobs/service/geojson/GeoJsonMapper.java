package app.bpartners.geojobs.service.geojson;

import static app.bpartners.geojobs.endpoint.rest.model.MultiPolygon.TypeEnum.MULTI_POLYGON;
import static app.bpartners.geojobs.service.geojson.GeoReferencer.toGeographicalCoordinates;

import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.repository.model.detection.DetectedObject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class GeoJsonMapper {

  public List<GeoJson.GeoFeature> toGeoFeatures(
      int xTile, int yTile, int zoom, int imageWidth, List<DetectedObject> detectedObjects) {
    var geoFeatures = new ArrayList<GeoJson.GeoFeature>();
    detectedObjects.forEach(
        object -> {
          var feature = object.getFeature();
          var geometry = feature.getGeometry();
          if (geometry == null || geometry.getCoordinates() == null) {
            throw new IllegalArgumentException("Multipolygon coordinates should not be null");
          }
          var geoFeature =
              mapToFeature(xTile, yTile, zoom, imageWidth, object, geometry.getCoordinates());
          geoFeatures.add(geoFeature);
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
    var properties = new HashMap<String, String>();
    properties.put("confidence", object.getComputedConfidence().toString());
    properties.put("label", object.getDetectedObjectType().getDetectableType().name());
    var multipolygon = new MultiPolygon();
    List<List<BigDecimal>> coordinates =
        geometryCoordinates.stream()
            .flatMap(List::stream)
            .flatMap(List::stream)
            .map(
                coor -> {
                  var x = coor.getFirst().doubleValue();
                  var y = -coor.getLast().doubleValue();
                  return toGeographicalCoordinates(xTile, yTile, x, y, zoom, imageWidth);
                })
            .toList();
    multipolygon.setType(MULTI_POLYGON);
    multipolygon.setCoordinates(List.of(List.of(coordinates)));
    return new GeoJson.GeoFeature(properties, multipolygon);
  }
}
