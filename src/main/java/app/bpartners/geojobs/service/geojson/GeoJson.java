package app.bpartners.geojobs.service.geojson;

import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class GeoJson implements Serializable {
  private final String stringValue;

  public GeoJson(List<GeoFeature> features) {
    stringValue = geojsonString(features);
  }

  private static String geojsonString(List<GeoFeature> geoFeatures) {
    ObjectMapper objectMapper = new ObjectMapper();

    Map<String, Object> geoJson = new HashMap<>();
    geoJson.put("type", "FeatureCollection");

    List<Map<String, Object>> features = new ArrayList<>();

    for (var geoF : geoFeatures) {
      Map<String, Object> featureAsMap = new HashMap<>();
      var geometry = geoF.getGeometry();
      var coordinates = geometry.getCoordinates();

      Map<String, Object> geometryAsMap = new HashMap<>();
      featureAsMap.put("type", "Feature");
      geometryAsMap.put("type", geometry.getType());
      geometryAsMap.put("coordinates", coordinates);

      featureAsMap.put("geometry", geometryAsMap);
      featureAsMap.put("properties", geoF.getProperties());

      features.add(featureAsMap);
    }

    geoJson.put("features", geoFeatures);

    try {
      return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(geoJson);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  @AllArgsConstructor
  @Getter
  @Setter
  @ToString
  @EqualsAndHashCode
  public static class GeoFeature implements Serializable {
    private static final String DEFAULT_FEATURE_TYPE = "Feature";
    private Map<String, String> properties;
    private String type;
    private MultiPolygon geometry;

    public GeoFeature(Map<String, String> properties, MultiPolygon geometry) {
      this.properties = properties;
      this.type = DEFAULT_FEATURE_TYPE;
      this.geometry = geometry;
    }
  }
}
