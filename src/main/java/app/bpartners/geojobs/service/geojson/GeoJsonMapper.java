package app.bpartners.geojobs.service.geojson;

import static app.bpartners.geojobs.endpoint.rest.model.MultiPolygon.TypeEnum.MULTIPOLYGON;
import static app.bpartners.geojobs.service.geojson.GeoReferencer.toGeographicalCoordinates;

import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.repository.model.detection.HumanDetectedObject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class GeoJsonMapper {

  public List<GeoJson.GeoFeature> toGeoFeatures(
      int xTile, int yTile, int zoom, int imageWidth, List<HumanDetectedObject> detectedObjects) {
    var geoFeatures = new ArrayList<GeoJson.GeoFeature>();
    detectedObjects.forEach(
        object -> {
          var feature = object.getFeature();
          if (feature == null || feature.getPoints() == null) {
            throw new IllegalArgumentException("Multipolygon coordinates should not be null");
          }
          var properties = new HashMap<String, String>();
          properties.put("confidence", object.getConfidence());
          properties.put("label", object.getLabel().getName());
          var multipolygon = new MultiPolygon();
          List<List<BigDecimal>> coordinates =
              feature.getPoints().stream()
                  .map(
                      coor -> {
                        var x = coor.getX();
                        var y = coor.getY();
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
