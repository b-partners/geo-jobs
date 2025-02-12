package app.bpartners.geojobs.endpoint.rest.postprocessing;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static java.util.stream.Collectors.toSet;

import app.bpartners.geojobs.endpoint.rest.postprocessing.model.LatLonPolygon;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

public class Geojson {

  @Accessors(fluent = true)
  @Getter
  private final String stringValue;

  public Geojson(Set<LatLonPolygon> polygons) {
    this.stringValue =
        geojsonString(
            polygons.stream().map(LatLonPolygon::polygon).map(Polygon::toString).collect(toSet()));
  }

  // Mostly ChatGPT-generated
  private static String geojsonString(Set<String> wktPolygons) {
    WKTReader reader = new WKTReader(geometryFactory);
    ObjectMapper objectMapper = new ObjectMapper();

    // GeoJSON structure
    Map<String, Object> geoJson = new HashMap<>();
    geoJson.put("type", "FeatureCollection");

    List<Map<String, Object>> features = new ArrayList<>();

    for (String wkt : wktPolygons) {
      Polygon polygon;
      try {
        polygon = (Polygon) reader.read(wkt);
      } catch (ParseException e) {
        throw new RuntimeException(e);
      }

      // Convert JTS Polygon to GeoJSON format
      List<List<List<Double>>> coordinates = new ArrayList<>();
      List<List<Double>> outerRing = new ArrayList<>();

      for (Coordinate coord : polygon.getExteriorRing().getCoordinates()) {
        outerRing.add(Arrays.asList(coord.y, coord.x));
      }
      coordinates.add(outerRing);

      // Handle holes (interior rings)
      for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
        List<List<Double>> hole = new ArrayList<>();
        for (Coordinate coord : polygon.getInteriorRingN(i).getCoordinates()) {
          hole.add(Arrays.asList(coord.x, coord.y));
        }
        coordinates.add(hole);
      }

      Map<String, Object> feature = new HashMap<>();
      feature.put("type", "Feature");

      Map<String, Object> geometry = new HashMap<>();
      geometry.put("type", "Polygon");
      geometry.put("coordinates", coordinates);

      feature.put("geometry", geometry);
      feature.put("properties", new HashMap<>()); // Empty properties

      features.add(feature);
    }

    geoJson.put("features", features);

    // Convert map to JSON string
    try {
      return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(geoJson);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
