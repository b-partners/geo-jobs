package app.bpartners.geojobs.service.annotator;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.ROOF;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.SOLAR_PANEL;
import static app.bpartners.geojobs.service.event.ZoneDetectionJobSucceededServiceTest.LAYER_20_10_1_PNG;
import static app.bpartners.geojobs.service.event.ZoneDetectionJobSucceededServiceTest.MOCK_JOB_ID;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.gen.annotator.endpoint.rest.model.*;
import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectType;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import app.bpartners.geojobs.repository.model.detection.DetectedObject;
import app.bpartners.geojobs.repository.model.detection.DetectedTile;
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
  @Autowired private ObjectMapper om;
  @Autowired private LabelExtractor labelExtractor;
  @Autowired PolygonExtractor polygonExtractor;
  @Autowired CreateAnnotationBatchExtractor createAnnotationBatchExtractor;

  private Feature feature;

  public static DetectedTile detectedTile(List<DetectedObject> detectedObjects) {
    return DetectedTile.builder()
        .id(randomUUID().toString())
        .bucketPath(LAYER_20_10_1_PNG)
        .tile(Tile.builder().build())
        .jobId(MOCK_JOB_ID)
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
        .detectedObjectTypes(detectedObjectType(id, type))
        .feature(feature)
        .computedConfidence(1.0)
        .build();
  }

  private static List<DetectableObjectType> detectedObjectType(String id, DetectableType type) {
    return List.of(DetectableObjectType.builder().objectId(id).detectableType(type).build());
  }

  @BeforeEach
  void setup() throws JsonProcessingException {
    feature = om.readValue(MOCK_FEATURE_AS_STRING, Feature.class);
  }

  @Test
  void extract_label_ok() {
    DetectableType roof = ROOF;
    String roofColor = "#DFFF00";
    Label expected = new Label().id(null).name(roof.name()).color(roofColor);

    Label actual = labelExtractor.apply(roof);
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
    DetectedObject detectedObject =
        DetectedObject.builder()
            .detectedObjectTypes(
                List.of(DetectableObjectType.builder().detectableType(ROOF).build()))
            .feature(feature)
            .build();

    Polygon actual = polygonExtractor.apply(detectedObject);

    assertEquals(expected, actual);
  }

  private static Polygon getFeaturePolygon() {
    return new Polygon().points(List.of(new Point().x(45.904988912620688).y(4.459648282829194)));
  }

  @Test
  void extract_annotation_batch_ok() {
    CreateAnnotationBatch expected =
        new CreateAnnotationBatch()
            .annotations(
                List.of(
                    new AnnotationBaseFields()
                        .userId("dummy")
                        .label(labelExtractor.apply(ROOF))
                        .polygon(getFeaturePolygon())));

    CreateAnnotationBatch actual =
        createAnnotationBatchExtractor.apply(
            detectedTile(List.of(inDoubtDetectedObject(ROOF))), "dummy", "dummy");

    assertEquals(ignoreGeneratedValues(expected), ignoreGeneratedValues(actual));
  }

  Label roof() {
    return new Label().name(ROOF.name());
  }

  Label solarPanel() {
    return new Label().name(SOLAR_PANEL.name());
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
