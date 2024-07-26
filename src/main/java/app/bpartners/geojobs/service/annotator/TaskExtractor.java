package app.bpartners.geojobs.service.annotator;

import app.bpartners.gen.annotator.endpoint.rest.model.CreateAnnotatedTask;
import app.bpartners.gen.annotator.endpoint.rest.model.Label;
import app.bpartners.geojobs.repository.model.detection.MachineDetectedTile;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
// TODO: check the appropriate Java function to implement
public class TaskExtractor {
  private final CreateAnnotationBatchExtractor createAnnotationBatchExtractor;
  private final LabelExtractor labelExtractor;

  private CreateAnnotatedTask extractTask(
      MachineDetectedTile machineDetectedTile, String annotatorId, List<Label> existingLabels) {
    return new CreateAnnotatedTask()
        .id(machineDetectedTile.getId())
        .annotatorId(annotatorId)
        .filename(machineDetectedTile.getBucketPath())
        .annotationBatch(
            createAnnotationBatchExtractor.apply(machineDetectedTile, annotatorId, existingLabels));
  }

  public List<CreateAnnotatedTask> apply(
      List<MachineDetectedTile> machineDetectedTiles,
      String annotatorId,
      List<Label> expectedLabels) {
    var existingLabels = labelExtractor.createUniqueLabelListFrom(machineDetectedTiles);
    return machineDetectedTiles.stream()
        .map(
            tile ->
                extractTask(
                    tile, annotatorId, existingLabels.isEmpty() ? expectedLabels : existingLabels))
        .toList();
  }
}
