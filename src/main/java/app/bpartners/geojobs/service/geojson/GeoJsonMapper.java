package app.bpartners.geojobs.service.geojson;

import static app.bpartners.geojobs.endpoint.rest.model.MultiPolygon.TypeEnum.MULTIPOLYGON;
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
          var properties = new HashMap<String, String>();
          properties.put("confidence", object.getComputedConfidence().toString());
          properties.put("label", object.getDetectedObjectType().getDetectableType().name());
          var multipolygon = new MultiPolygon();
          List<List<BigDecimal>> coordinates =
              geometry.getCoordinates().stream()
                  .flatMap(List::stream)
                  .flatMap(List::stream)
                  .map(
                      coor -> {
                        var x = coor.getFirst().doubleValue();
                        var y = coor.getLast().doubleValue();
                        return toGeographicalCoordinates(xTile, yTile, x, y, zoom, imageWidth);
                      })
                  .toList();
          multipolygon.setType(MULTIPOLYGON);
          multipolygon.setCoordinates(List.of(List.of(coordinates)));
          var geoFeature = new GeoJson.GeoFeature(properties, multipolygon);
          geoFeatures.add(geoFeature);
        });
    return geoFeatures;
  }
}
