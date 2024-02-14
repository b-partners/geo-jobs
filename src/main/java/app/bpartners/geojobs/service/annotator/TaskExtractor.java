package app.bpartners.geojobs.service.annotator;

import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.repository.annotator.gen.AnnotatedTask;
import app.bpartners.geojobs.repository.model.detection.DetectedTile;
import java.util.List;
import java.util.function.BiFunction;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class TaskExtractor implements BiFunction<List<DetectedTile>, String, List<AnnotatedTask>> {
  private final AnnotationBatchExtractor annotationBatchExtractor;

  public static String formatToAnnotatorFilePath(String geoJobsFilePath) {
    return geoJobsFilePath.replaceAll("/", "_");
  }

  private AnnotatedTask extractTask(DetectedTile detectedTile, String annotatorId) {
    String taskId = randomUUID().toString();
    return AnnotatedTask.builder()
        .id(taskId)
        .filename(formatToAnnotatorFilePath(detectedTile.getBucketPath()))
        .annotationBatch(annotationBatchExtractor.apply(detectedTile, annotatorId, taskId))
        .build();
  }

  @Override
  public List<AnnotatedTask> apply(List<DetectedTile> detectedTiles, String annotatorId) {
    return detectedTiles.stream().map(tile -> extractTask(tile, annotatorId)).toList();
  }
}
