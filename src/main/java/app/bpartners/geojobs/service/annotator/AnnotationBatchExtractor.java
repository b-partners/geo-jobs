package app.bpartners.geojobs.service.annotator;

import static java.util.UUID.randomUUID;

import app.bpartners.annotator.endpoint.rest.model.Annotation;
import app.bpartners.annotator.endpoint.rest.model.AnnotationBatch;
import app.bpartners.geojobs.repository.model.detection.DetectedObject;
import app.bpartners.geojobs.repository.model.detection.DetectedTile;
import java.time.Instant;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AnnotationBatchExtractor {
  private final LabelExtractor labelExtractor;
  private final PolygonExtractor polygonExtractor;

  public AnnotationBatch apply(DetectedTile detectedTile, String annotatorId, String taskId) {
    return new AnnotationBatch()
        .id(randomUUID().toString())
        .creationDatetime(Instant.now())
        .annotations(
            detectedTile.getDetectedObjects().stream()
                .map(detectedObject -> extractAnnotation(detectedObject, annotatorId, taskId))
                .toList());
  }

  private Annotation extractAnnotation(
      DetectedObject detectedObject, String annotatorId, String taskId) {
    return new Annotation()
        .id(randomUUID().toString())
        .userId(annotatorId)
        .taskId(taskId)
        .label(
            labelExtractor.apply(
                detectedObject.getDetectedObjectTypes().get(0).getDetectableType()))
        .polygon(polygonExtractor.apply(detectedObject));
  }
}
