package app.bpartners.geojobs.service.cityjson.exporter;


import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.Collection;

import static java.util.UUID.randomUUID;

@Slf4j
public class CityJSONStepExporter {
  private final String crs;
  private final File directory;
  private final ObjectMapper objectMapper;
  private static final String GEO_JSON_FILE_EXTENSION = "geojson";

  public CityJSONStepExporter(File directory, String crs, ObjectMapper objectMapper) {
    this.crs = crs;
    this.directory = directory;
    this.objectMapper = objectMapper;
  }


  public void export(CityJSONStep step, Collection<LasPointGeometry> points) {
    var features = objectMapper.createArrayNode();

    for (var p : points) {
      var coordinates = coordinates(p.getX(), p.getY(), p.getZ());
      var pointGeometry = pointGeometry(coordinates);
      var feature = feature(pointGeometry);

      features.add(feature);
    }

    write(step, randomUUID().toString(), featureCollection(features));
  }

  private File toFile(CityJSONStep step, String suffix) {
    return directory
        .toPath()
        .resolve(String.format("%s_%s.%s", step.toString(), suffix, GEO_JSON_FILE_EXTENSION))
        .toFile();
  }

  private void write(CityJSONStep step, String suffix, ObjectNode featureCollection) {
    try {
      var file =  toFile(step, suffix);
      log.info("Exporting CityJSON step '{}' to file: {}", step, file.getAbsolutePath());

      objectMapper
          .writerWithDefaultPrettyPrinter()
          .writeValue(toFile(step, suffix), featureCollection);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private ObjectNode crs() {
    var crsNode = objectMapper.createObjectNode();
    crsNode.put("type", "name");

    var props = objectMapper.createObjectNode();
    props.put("name", crs);
    crsNode.set("properties", props);

    return crsNode;
  }

  private ObjectNode featureCollection(ArrayNode features) {
    var root = objectMapper.createObjectNode();

    root.put("type", "FeatureCollection");
    root.set("crs", crs());
    root.set("features", features);

    return root;
  }

  private ObjectNode feature(ObjectNode geometry) {
    var feature = objectMapper.createObjectNode();

    feature.put("type", "Feature");
    feature.set("geometry", geometry);
    feature.set("properties", objectMapper.createObjectNode());

    return feature;
  }

  private ObjectNode pointGeometry(ArrayNode coordinates) {
    var geometry = objectMapper.createObjectNode();

    geometry.put("type", "Point");
    geometry.set("coordinates", coordinates);

    return geometry;
  }

  private ArrayNode coordinates(Double x, Double y, Double z) {
    var coordinates = objectMapper.createArrayNode();

    coordinates.add(x);
    coordinates.add(y);
    if (z != null) {
      coordinates.add(z);
    }

    return coordinates;
  }
}
