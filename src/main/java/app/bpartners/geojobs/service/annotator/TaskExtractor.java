package app.bpartners.geojobs.service.annotator;

import static java.util.UUID.randomUUID;

import app.bpartners.gen.annotator.endpoint.rest.model.CreateAnnotatedTask;
import app.bpartners.gen.annotator.endpoint.rest.model.Label;
import app.bpartners.geojobs.repository.model.detection.DetectedTile;
import java.util.List;
import java.util.function.BiFunction;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class TaskExtractor
    implements BiFunction<List<DetectedTile>, String, List<CreateAnnotatedTask>> {
  private final CreateAnnotationBatchExtractor createAnnotationBatchExtractor;
  private final LabelExtractor labelExtractor;

  private CreateAnnotatedTask extractTask(
      DetectedTile detectedTile, String annotatorId, List<Label> existingLabels) {
    String taskId = randomUUID().toString();
    return new CreateAnnotatedTask()
        .id(taskId)
        .annotatorId(annotatorId)
        .filename(detectedTile.getBucketPath())
        .annotationBatch(
            createAnnotationBatchExtractor.apply(
                detectedTile, annotatorId, taskId, existingLabels));
  }

  @Override
  public List<CreateAnnotatedTask> apply(List<DetectedTile> detectedTiles, String annotatorId) {
    var existingLabels = labelExtractor.createUniqueLabelListFrom(detectedTiles);
    return detectedTiles.stream()
        .map(tile -> extractTask(tile, annotatorId, existingLabels))
        .toList();
  }
}
