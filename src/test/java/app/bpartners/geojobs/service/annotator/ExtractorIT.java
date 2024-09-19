package app.bpartners.geojobs.service.annotator;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.PANNEAU_PHOTOVOLTAIQUE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.PASSAGE_PIETON;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.PISCINE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.TOITURE_REVETEMENT;
import static app.bpartners.geojobs.service.AnnotationServiceIT.inDoubtTile;
import static app.bpartners.geojobs.service.event.ZoneDetectionAnnotationProcessorTest.LAYER_20_10_1_PNG;
import static app.bpartners.geojobs.service.event.ZoneDetectionAnnotationProcessorTest.MOCK_JOB_ID;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.gen.annotator.endpoint.rest.model.*;
import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectType;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import app.bpartners.geojobs.repository.model.detection.DetectedObject;
import app.bpartners.geojobs.repository.model.detection.MachineDetectedTile;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ExtractorIT extends FacadeIT {
  private static final String MOCK_FEATURE_AS_STRING =
      """
      { "type": "Feature",
        "properties": {
          "code": "69",
          "nom": "Rhône",
          "id": 30251921,
          "CLUSTER_ID": 99520,
          "CLUSTER_SIZE": 386884 },
        "geometry": {
          "type": "MultiPolygon",
          "coordinates": [ [ [
            [ 4.459648282829194, 45.904988912620688 ]
            ] ] ] } }""";
  public static final String PARCEL_MOCK_ID = "parcel1";
  @Autowired ObjectMapper om;
  @Autowired LabelConverter labelConverter;
  @Autowired LabelExtractor labelExtractor;
  @Autowired PolygonExtractor polygonExtractor;
  @Autowired CreateAnnotationBatchExtractor createAnnotationBatchExtractor;

  private Feature feature;

  public static MachineDetectedTile detectedTile(List<DetectedObject> detectedObjects) {
    return MachineDetectedTile.builder()
        .id(randomUUID().toString())
        .bucketPath(LAYER_20_10_1_PNG)
        .tile(Tile.builder().build())
        .zdjJobId(MOCK_JOB_ID)
        .parcelId(PARCEL_MOCK_ID)
        .creationDatetime(Instant.now())
        .detectedObjects(detectedObjects)
        .build();
  }

  @SneakyThrows
  DetectedObject inDoubtDetectedObject(DetectableType type) {
    String id = randomUUID().toString();
    return DetectedObject.builder()
        .id(id)
        .detectedObjectType(detectedObjectType(id, type))
        .feature(feature)
        .computedConfidence(1.0)
        .build();
  }

  private DetectableObjectType detectedObjectType(String id, DetectableType type) {
    return DetectableObjectType.builder().objectId(id).detectableType(type).build();
  }

  @BeforeEach
  void setup() throws JsonProcessingException {
    feature = om.readValue(MOCK_FEATURE_AS_STRING, Feature.class);
  }

  @Test
  void extract_label_ok() {
    DetectableType roof = TOITURE_REVETEMENT;
    String roofColor = "#DFFF00";
    Label expected = new Label().id(null).name(roof.name()).color(roofColor);

    Label actual = labelConverter.apply(roof);
    actual.setId(null);

    assertEquals(expected, actual);
  }

  @Test
  void extract_labels_from_task_ok() {
    List<Label> expected = List.of(roof(), solarPanel());
    CreateAnnotatedTask annotatedTask =
        new CreateAnnotatedTask()
            .annotationBatch(
                new CreateAnnotationBatch()
                    .annotations(
                        List.of(
                            new AnnotationBaseFields().label(roof()),
                            new AnnotationBaseFields().label(roof()),
                            new AnnotationBaseFields().label(solarPanel()))));

    List<Label> actual = labelExtractor.extractLabelsFromTasks(List.of(annotatedTask));

    assertEquals(expected.size(), actual.size());
    assertTrue(expected.containsAll(actual));
  }

  @Test
  void extract_polygon_ok() {
    Polygon expected = getFeaturePolygon();
    DetectedObject machineDetectedObject =
        DetectedObject.builder()
            .detectedObjectType(
                DetectableObjectType.builder().detectableType(TOITURE_REVETEMENT).build())
            .feature(feature)
            .build();

    Polygon actual = polygonExtractor.apply(machineDetectedObject);

    assertEquals(expected, actual);
  }

  @Test
  void get_unique_labels_from_detected_tiles() {
    var messyListOfTiles =
        List.of(
            inDoubtTile(null, null, null, null, PASSAGE_PIETON),
            inDoubtTile(null, null, null, null, PISCINE),
            inDoubtTile(null, null, null, null, PISCINE),
            inDoubtTile(null, null, null, null, PASSAGE_PIETON),
            inDoubtTile(null, null, null, null, PISCINE),
            inDoubtTile(null, null, null, null, PISCINE),
            inDoubtTile(null, null, null, null, TOITURE_REVETEMENT),
            inDoubtTile(null, null, null, null, TOITURE_REVETEMENT),
            inDoubtTile(null, null, null, null, TOITURE_REVETEMENT),
            inDoubtTile(null, null, null, null, PASSAGE_PIETON),
            inDoubtTile(null, null, null, null, TOITURE_REVETEMENT),
            inDoubtTile(null, null, null, null, TOITURE_REVETEMENT),
            inDoubtTile(null, null, null, null, TOITURE_REVETEMENT),
            inDoubtTile(null, null, null, null, PASSAGE_PIETON),
            inDoubtTile(null, null, null, null, PASSAGE_PIETON),
            inDoubtTile(null, null, null, null, PASSAGE_PIETON),
            inDoubtTile(null, null, null, null, PASSAGE_PIETON),
            inDoubtTile(null, null, null, null, TOITURE_REVETEMENT));
    var expected = List.of(pathWay(), roof(), pool());

    List<Label> actual = labelExtractor.createUniqueLabelListFrom(messyListOfTiles);

    assertEquals(expected.size(), actual.size());
    assertTrue(
        expected.containsAll(actual.stream().map(ExtractorIT::ignoreGeneratedValuesOf).toList()));
  }

  private static Label ignoreGeneratedValuesOf(Label label) {
    return label.id(null).color(null);
  }

  private static Polygon getFeaturePolygon() {
    return new Polygon().points(List.of(new Point().x(4.459648282829194).y(45.904988912620688)));
  }

  @Test
  void extract_annotation_batch_ok() {
    Label label = labelConverter.apply(TOITURE_REVETEMENT);
    DetectedObject machineDetectedObject = inDoubtDetectedObject(TOITURE_REVETEMENT);
    CreateAnnotationBatch expected =
        new CreateAnnotationBatch()
            .annotations(
                List.of(
                    new AnnotationBaseFields()
                        .userId("dummy")
                        .label(label)
                        .comment(
                            "confidence=" + machineDetectedObject.getComputedConfidence() * 100)
                        .polygon(getFeaturePolygon())));

    CreateAnnotationBatch actual =
        createAnnotationBatchExtractor.apply(
            detectedTile(List.of(machineDetectedObject)), "dummy", List.of(label));

    assertEquals(ignoreGeneratedValues(expected), ignoreGeneratedValues(actual));
  }

  Label roof() {
    return new Label().name(TOITURE_REVETEMENT.name());
  }

  Label solarPanel() {
    return new Label().name(PANNEAU_PHOTOVOLTAIQUE.name());
  }

  Label pathWay() {
    return new Label().name(PASSAGE_PIETON.name());
  }

  Label pool() {
    return new Label().name(PISCINE.name());
  }

  CreateAnnotationBatch ignoreGeneratedValues(CreateAnnotationBatch annotationBatch) {
    List<AnnotationBaseFields> annotations = annotationBatch.getAnnotations();
    annotations.forEach(
        a -> {
          a.setId(null);
          Label label = a.getLabel();
          label.setId(null);
          a.setLabel(label);
        });
    annotationBatch.setAnnotations(annotations);
    annotationBatch.setId(null);
    annotationBatch.setCreationDatetime(null);
    return annotationBatch;
  }
}
