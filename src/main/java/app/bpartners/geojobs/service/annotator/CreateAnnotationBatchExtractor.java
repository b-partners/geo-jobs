package app.bpartners.geojobs.service.annotator;

import static java.util.UUID.randomUUID;

import app.bpartners.gen.annotator.endpoint.rest.model.AnnotationBaseFields;
import app.bpartners.gen.annotator.endpoint.rest.model.CreateAnnotationBatch;
import app.bpartners.gen.annotator.endpoint.rest.model.Label;
import app.bpartners.geojobs.repository.model.detection.MachineDetectedObject;
import app.bpartners.geojobs.repository.model.detection.MachineDetectedTile;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class CreateAnnotationBatchExtractor {
  private final LabelExtractor labelExtractor;
  private final PolygonExtractor polygonExtractor;

  public CreateAnnotationBatch apply(
      MachineDetectedTile machineDetectedTile,
      String annotatorId,
      String taskId,
      List<Label> existingLabels) {
    CreateAnnotationBatch annotations =
        new CreateAnnotationBatch()
            .id(randomUUID().toString())
            .creationDatetime(Instant.now())
            .annotations(
                machineDetectedTile.getMachineDetectedObjects().stream()
                    .map(
                        detectedObject ->
                            extractAnnotation(detectedObject, annotatorId, existingLabels))
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
      MachineDetectedObject machineDetectedObject, String annotatorId, List<Label> existingLabels) {
    var label =
        labelExtractor.findLabelByNameFromList(
            existingLabels, machineDetectedObject.getDetectableObjectType().name());
    return new AnnotationBaseFields()
        .id(randomUUID().toString())
        .userId(annotatorId)
        .label(label)
        .comment("confidence=" + machineDetectedObject.getComputedConfidence() * 100)
        .polygon(polygonExtractor.apply(machineDetectedObject));
  }
}
