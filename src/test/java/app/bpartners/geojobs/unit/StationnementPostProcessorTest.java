package app.bpartners.geojobs.unit;

import static app.bpartners.geojobs.endpoint.rest.model.ModelName.STATIONNEMENT;
import static app.bpartners.geojobs.endpoint.rest.model.ModelName.TOITURE;
import static app.bpartners.geojobs.postprocessing.StationnementPostProcessor.MIN_PLACE_STANDARD_AREA_IN_SQUARE_METER;
import static app.bpartners.geojobs.service.geojson.GeoJson.fromFeatures;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectModel;
import app.bpartners.geojobs.endpoint.rest.model.ModelName;
import app.bpartners.geojobs.postprocessing.StationnementPostProcessor;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.service.GeometrySquareMeterArea;
import app.bpartners.geojobs.service.geojson.GeoJson;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class StationnementPostProcessorTest {
  private static final String RESULT_GEOJSON =
      "/stationnement_toiture/Result_BP_TOITURE_+BP_STATIONNEMENT.geojson";
  private static final String GROUND_TRUTH_GEOJSON =
      "/stationnement_toiture/Ground_truth_BP_TOITURE_+BP_STATIONNEMENT.geojson";
  private static final String PLACE_STANDARD_LABEL = "PLACE_STANDARD";

  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
  private final GeometryConverter geometryConverter = new GeometryConverter();
  private final GeometrySquareMeterArea geometrySquareMeterArea = new GeometrySquareMeterArea();
  private final StationnementPostProcessor subject =
      new StationnementPostProcessor(geometryConverter, geometrySquareMeterArea);

  @Test
  void filters_out_noisy_place_standard_objects_of_stationnement_model()
      throws IOException, URISyntaxException {
    var detectedPlaceStandard = labeledGeoFeatures(RESULT_GEOJSON, PLACE_STANDARD_LABEL);
    var manuallyRemovedGeometries = new HashSet<>(geometriesOf(detectedPlaceStandard));
    manuallyRemovedGeometries.removeAll(
        geometriesOf(labeledGeoFeatures(GROUND_TRUTH_GEOJSON, PLACE_STANDARD_LABEL)));

    var actual = subject.apply(fromFeatures(detectedPlaceStandard), detection(STATIONNEMENT));

    assertFalse(manuallyRemovedGeometries.isEmpty(), "noisy objects are expected in the fixture");
    var actualGeometries = new HashSet<>(geometriesOf(actual.getGeoFeatures()));
    manuallyRemovedGeometries.forEach(
        noisyGeometry ->
            assertFalse(
                actualGeometries.contains(noisyGeometry),
                "noisy PLACE_STANDARD is expected to be filtered out"));
    detectedPlaceStandard.stream()
        .filter(
            geoFeature -> areaInSquareMeter(geoFeature) >= MIN_PLACE_STANDARD_AREA_IN_SQUARE_METER)
        .forEach(
            wideEnough ->
                assertTrue(
                    actualGeometries.contains(geometryOf(wideEnough)),
                    "PLACE_STANDARD wide enough is expected to be kept"));
  }

  @Test
  void does_not_filter_out_anything_when_model_is_not_stationnement()
      throws IOException, URISyntaxException {
    var detectedPlaceStandard = labeledGeoFeatures(RESULT_GEOJSON, PLACE_STANDARD_LABEL);
    var geoJson = fromFeatures(detectedPlaceStandard);

    assertEquals(geoJson, subject.apply(geoJson, detection(TOITURE)));
    assertEquals(geoJson, subject.apply(geoJson, null));
  }

  @Test
  void does_not_filter_out_objects_other_than_place_standard()
      throws IOException, URISyntaxException {
    var detectedParking = labeledGeoFeatures(RESULT_GEOJSON, "PARKING");
    var geoJson = fromFeatures(detectedParking);

    var actual = subject.apply(geoJson, detection(STATIONNEMENT));

    assertEquals(detectedParking.size(), actual.getGeoFeatures().size());
  }

  private Detection detection(ModelName modelName) {
    return Detection.builder()
        .id("detection_id")
        .detectableObjectModelList(List.of(new DetectableObjectModel().modelName(modelName)))
        .build();
  }

  private double areaInSquareMeter(GeoJson.GeoFeature geoFeature) {
    return geometrySquareMeterArea.apply(
        geometryConverter.apply(geoFeature.getGeometry().getCoordinates()));
  }

  private Set<String> geometriesOf(List<GeoJson.GeoFeature> geoFeatures) {
    return geoFeatures.stream().map(this::geometryOf).collect(HashSet::new, Set::add, Set::addAll);
  }

  private String geometryOf(GeoJson.GeoFeature geoFeature) {
    return geometryConverter.apply(geoFeature.getGeometry().getCoordinates()).toText();
  }

  private List<GeoJson.GeoFeature> labeledGeoFeatures(String resourcePath, String label)
      throws IOException, URISyntaxException {
    var geoJsonPath = Paths.get(getClass().getResource(resourcePath).toURI());
    JsonNode featureCollection = objectMapper.readTree(Files.readString(geoJsonPath));
    List<GeoJson.GeoFeature> geoFeatures = new ArrayList<>();
    for (JsonNode feature : featureCollection.get("features")) {
      var properties = feature.get("properties");
      if (properties == null
          || properties.get("label") == null
          || !label.equals(properties.get("label").asText())) {
        continue;
      }
      geoFeatures.add(objectMapper.treeToValue(feature, GeoJson.GeoFeature.class));
    }
    return geoFeatures;
  }
}
