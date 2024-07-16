package app.bpartners.geojobs.service.geojson;

import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static app.bpartners.geojobs.endpoint.rest.model.MultiPolygon.TypeEnum.MULTIPOLYGON;
import static app.bpartners.geojobs.service.geojson.GeoReferencer.toGeographicalCoordinates;

@Component
public class GeoJsonMapper {

  public List<GeoJson.GeoFeature> toGeoFeatures(int xTile, int yTile, int zoom, int imageWidth, List<Feature> features) {
    var geoFeatures = new ArrayList<GeoJson.GeoFeature>();
    features.forEach(feature -> {
      var geometry = feature.getGeometry();
      if (geometry == null || geometry.getCoordinates() == null){
        throw new IllegalArgumentException("Multipolygon coordinates should not be null");
      }
      var properties = new HashMap<String, String>();
      properties.put("id", feature.getId());
      properties.put("confidence", "0.95");

      var multipolygon = new MultiPolygon();

      List<List<List<BigDecimal>>> coordinates = geometry.getCoordinates()
          .stream()
          .map((cor) -> cor.getFirst().stream().map(subcor -> {
            var x = subcor.getFirst().doubleValue();
            var y = subcor.getLast().doubleValue();
            return toGeographicalCoordinates(xTile, yTile, x, y, zoom, imageWidth);
          }).toList()).toList();

      multipolygon.setType(MULTIPOLYGON);
      multipolygon.setCoordinates(List.of(coordinates));
      var geoFeature = new GeoJson.GeoFeature(properties, multipolygon);
      geoFeatures.add(geoFeature);
    });
    return geoFeatures;
  }
}
