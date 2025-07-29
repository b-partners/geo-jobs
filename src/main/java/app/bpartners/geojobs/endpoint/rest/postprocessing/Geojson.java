package app.bpartners.geojobs.endpoint.rest.postprocessing;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.util.stream.Collectors.toSet;

import app.bpartners.geojobs.endpoint.rest.postprocessing.model.LatLonPolygon;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.geojson.feature.FeatureJSON;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

@Accessors(fluent = true)
@Getter
public class Geojson {

  private final String stringValue;
  private final Set<LatLonPolygon> polygons;

  public Geojson(Set<LatLonPolygon> polygons) {
    this.polygons = polygons;
    this.stringValue =
        geojsonString(polygons.stream().map(LatLonPolygon::polygon).collect(toSet()));
  }

  public Geojson(File geojsonPath) {
    this(latLonPolygon(geojsonPath));
  }

  private static Set<LatLonPolygon> latLonPolygon(File geojsonPath) {
    Set<LatLonPolygon> latLonPolygons = new HashSet<>();
    FeatureJSON featureJson = new FeatureJSON();

    try (FileReader reader = new FileReader(geojsonPath);
        var featuresIterator = featureJson.readFeatureCollection(reader).features()) {

      while (featuresIterator.hasNext()) {
        SimpleFeature feature = (SimpleFeature) featuresIterator.next();
        try {
          Polygon polygon = getPolygon(feature);
          latLonPolygons.add(new LatLonPolygon(polygon));
        } catch (IllegalArgumentException ex) {
          throw new IllegalStateException("Failed to process feature: " + feature.getID(), ex);
        }
      }

    } catch (IOException ioEx) {
      throw new UncheckedIOException(
          "Failed to read GeoJSON file: " + geojsonPath.getAbsolutePath(), ioEx);
    }
    return latLonPolygons;
  }

  private static Polygon getPolygon(SimpleFeature feature) {
    Object confidence = getPropertySafe(feature, "confidence");
    Object label = getPropertySafe(feature, "label");

    Polygon polygon = extractPolygonGeometry(feature);

    if (!polygon.isValid())
      throw new IllegalArgumentException("Invalid polygon geometry: " + polygon.toText());

    Map<String, Object> userData = new HashMap<>();
    if (label != null) userData.put("label", label);
    if (confidence != null) userData.put("confidence", confidence);

    polygon.setUserData(userData);
    return polygon;
  }

  private static Object getPropertySafe(SimpleFeature feature, String propName) {
    var property = feature.getProperty(propName);
    return (property != null) ? property.getValue() : null;
  }

  private static Polygon extractPolygonGeometry(SimpleFeature feature) {
    Geometry geometry = (Geometry) feature.getDefaultGeometry();
    if (geometry instanceof Polygon polygon) {
      return polygon;
    } else if (geometry instanceof MultiPolygon multiPolygon) {
      if (multiPolygon.getNumGeometries() != 1) {
        throw new IllegalArgumentException(
            "Unsupported: MultiPolygon with more than one geometry: " + multiPolygon);
      }
      return (Polygon) multiPolygon.getGeometryN(0);
    } else {
      throw new IllegalArgumentException(
          "Unsupported geometry type: " + geometry.getGeometryType());
    }
  }

  // Mostly ChatGPT-generated
  private static String geojsonString(Set<Polygon> wktPolygons) {
    ObjectMapper objectMapper = new ObjectMapper();

    // GeoJSON structure
    Map<String, Object> geoJson = new HashMap<>();
    geoJson.put("type", "FeatureCollection");

    List<Map<String, Object>> features = new ArrayList<>();

    for (Polygon polygon : wktPolygons) {
      var userData = (Map<String, String>) polygon.getUserData();
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
          hole.add(Arrays.asList(coord.y, coord.x));
        }
        coordinates.add(hole);
      }

      Map<String, Object> feature = new HashMap<>();
      feature.put("type", "Feature");

      Map<String, Object> geometry = new HashMap<>();
      geometry.put("type", "Polygon");
      geometry.put("coordinates", coordinates);

      feature.put("geometry", geometry);
      feature.put("properties", userData); // Empty userData

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

  public void saveAsFile(String outputFile) {
    try {
      Files.write(
          new File(outputFile).toPath(), this.stringValue.getBytes(), CREATE, TRUNCATE_EXISTING);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
