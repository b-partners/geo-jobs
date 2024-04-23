package app.bpartners.geojobs.service.annotator;

import static java.util.UUID.randomUUID;

import app.bpartners.gen.annotator.endpoint.rest.model.AnnotationBaseFields;
import app.bpartners.gen.annotator.endpoint.rest.model.CreateAnnotationBatch;
import app.bpartners.geojobs.repository.model.detection.DetectedObject;
import app.bpartners.geojobs.repository.model.detection.DetectedTile;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class CreateAnnotationBatchExtractor {
  private final LabelExtractor labelExtractor;
  private final PolygonExtractor polygonExtractor;

  public CreateAnnotationBatch apply(DetectedTile detectedTile, String annotatorId, String taskId) {
    CreateAnnotationBatch annotations =
        new CreateAnnotationBatch()
            .id(randomUUID().toString())
            .creationDatetime(Instant.now())
            .annotations(
                detectedTile.getDetectedObjects().stream()
                    .map(detectedObject -> extractAnnotation(detectedObject, annotatorId))
                    .toList());
    log.info(
        "[DEBUG] CreateAnnotationBatchExtractor Annotations [{}]",
        annotations.getAnnotations().stream()
            .map(
                annotation ->
                    "AnnotationBaseFields(id="
                        + annotation.getId()
                        + ", label="
                        + annotation.getLabel()
                        + ", polygonPointsSize="
                        + annotation.getPolygon().getPoints().size()
                        + ")")
            .toList());
    return annotations;
  }

  private AnnotationBaseFields extractAnnotation(
      DetectedObject detectedObject, String annotatorId) {
    return new AnnotationBaseFields()
        .id(randomUUID().toString())
        .userId(annotatorId)
        .label(labelExtractor.apply(detectedObject.getDetectableObjectType()))
        .polygon(polygonExtractor.apply(detectedObject));
  }
}
